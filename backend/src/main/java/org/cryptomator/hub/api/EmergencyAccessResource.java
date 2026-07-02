package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.http.HttpServerRequest;
import org.jspecify.annotations.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.EmergencyRecoveryProcess;
import org.cryptomator.hub.entities.RecoveredEmergencyKeyShares;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.filters.VaultRole;
import org.cryptomator.hub.util.RawJson;
import org.cryptomator.hub.validation.ValidJWE;
import org.cryptomator.hub.validation.ValidJWS;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/emergency-access")
public class EmergencyAccessResource {

	private final EmergencyRecoveryProcess.Repository recoverProcessRepo;
	private final RecoveredEmergencyKeyShares.Repository recoveredKeySharesRepo;
	private final JsonWebToken jwt;
	private final Vault.Repository vaultRepo;
	private final EventLogger eventLogger;

	@Context
	HttpServerRequest request;

	@Inject
	EmergencyAccessResource(EmergencyRecoveryProcess.Repository recoverProcessRepo, RecoveredEmergencyKeyShares.Repository recoveredKeySharesRepo, JsonWebToken jwt, Vault.Repository vaultRepo, EventLogger eventLogger) {
		this.recoverProcessRepo = recoverProcessRepo;
		this.recoveredKeySharesRepo = recoveredKeySharesRepo;
		this.jwt = jwt;
		this.vaultRepo = vaultRepo;
		this.eventLogger = eventLogger;
	}

	@PUT
	@Path("/{processId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "starts a new recovery process")
	@APIResponse(responseCode = "204", description = "process created")
	@APIResponse(responseCode = "400", description = "invalid request, e.g. missing required fields")
	@APIResponse(responseCode = "403", description = "current user is not a member of the vault's emergency access council")
	@Transactional
	public Response startRecovery(@PathParam("processId") UUID processId, @Valid RecoveryProcessDto dto) {
		var currentUser = jwt.getSubject();
		var vault = vaultRepo.findByIdOptional(dto.vaultId).orElseThrow(NotFoundException::new);
		if (!vault.getEmergencyKeyShares().containsKey(currentUser)) {
			// only current members of the vault's emergency access council may start a recovery process
			throw new ForbiddenException("User is not a member of the vault's emergency access council");
		}
		if (!dto.recoveredKeyShares.containsKey(currentUser)) {
			// the council member who starts the process must, by definition, be part of the process
			throw new BadRequestException("User is not a member of the recovery process");
		}

		var process = new EmergencyRecoveryProcess();
		process.setId(processId);
		process.setVaultId(dto.vaultId);
		process.setType(dto.type);
		process.setDetails(dto.details);
		process.setRequiredKeyShares(dto.requiredKeyShares);
		process.setProcessPublicKey(dto.processPublicKey);
		var keyShares = dto.recoveredKeyShares.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
			var memberId = e.getKey();
			var keyShareDto = e.getValue();
			var keyShareEntity = new RecoveredEmergencyKeyShares();
			keyShareEntity.setId(new RecoveredEmergencyKeyShares.Id(memberId, processId));
			keyShareEntity.setProcessPrivateKey(keyShareDto.processPrivateKey);
			keyShareEntity.setUnrecoveredKeyShare(keyShareDto.unrecoveredKeyShare);
			keyShareEntity.setRecoveredKeyShare(keyShareDto.recoveredKeyShare);
			keyShareEntity.setSignedProcessInfo(keyShareDto.signedProcessInfo);
			return keyShareEntity;
		}));
		process.setRecoveredKeyShares(keyShares);
		recoverProcessRepo.persist(process);

		// audit logging
		eventLogger.logEmergencyAccessRecoveryStarted(dto.vaultId, processId, currentUser, dto.type.name(), dto.details);
		var myKeyShare = keyShares.get(currentUser);
		assert myKeyShare != null; // as verified above, the user must be part of the process
		if (myKeyShare.getRecoveredKeyShare() != null) { // usually, this member also adds their key share immediately
			eventLogger.logEmergencyAccessRecoveryApproved(processId, currentUser, request.remoteAddress().hostAddress());
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@POST
	@Path("/{processId}/recovered-key-shares")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds recovered key share")
	@APIResponse(responseCode = "204", description = "key share added")
	@APIResponse(responseCode = "400", description = "invalid request, e.g. missing required fields")
	@Transactional
	public Response addRecoveredKeyShare(@PathParam("processId") UUID processId, RecoveredKeyShareDto dto) {
		var id = new RecoveredEmergencyKeyShares.Id(jwt.getSubject(), processId);

		var myKeyShare = recoveredKeySharesRepo.findById(id);
		if (myKeyShare == null) {
			throw new NotFoundException("Recovery process not found or user not part of emergency access council");
		}

		myKeyShare.setRecoveredKeyShare(dto.recoveredKeyShare);
		myKeyShare.setSignedProcessInfo(dto.signedProcessInfo);
		recoveredKeySharesRepo.persist(myKeyShare);

		// audit logging
		eventLogger.logEmergencyAccessRecoveryApproved(processId, jwt.getSubject(), request.remoteAddress().hostAddress());

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@DELETE
	@Path("/{processId}/complete")
	@RolesAllowed("user")
	@Operation(summary = "completes an existing recovery process")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404")
	@Transactional
	public Response complete(@PathParam("processId") UUID processId) {
		var currentUserId = jwt.getSubject();
		var ip = request.remoteAddress().hostAddress();
		var process = recoverProcessRepo.findByIdOptional(processId).orElseThrow(NotFoundException::new);
		if (!process.getRecoveredKeyShares().containsKey(currentUserId)) {
			throw new ForbiddenException();
		} else if (recoverProcessRepo.deleteById(processId)) {
			eventLogger.logEmergencyAccessRecoveryCompleted(processId, currentUserId, ip);
			return Response.noContent().build();
		} else {
			throw new NotFoundException();
		}
	}

	@DELETE
	@Path("/{processId}/abort")
	@RolesAllowed("user")
	@Operation(summary = "aborts an existing recovery process")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404")
	@Transactional
	public Response abort(@PathParam("processId") UUID processId) {
		var currentUserId = jwt.getSubject();
		var process = recoverProcessRepo.findByIdOptional(processId).orElseThrow(NotFoundException::new);
		if (process.getRecoveredKeyShares().containsKey(currentUserId) || vaultRepo.findById(process.getVaultId()).getEmergencyKeyShares().containsKey(currentUserId)) {
			eventLogger.logEmergencyAccessRecoveryAborted(process.getVaultId(), processId, currentUserId, request.remoteAddress().hostAddress());
			recoverProcessRepo.delete(process);
			return Response.noContent().build();
		} else {
			throw new ForbiddenException();
		}
	}

	@GET
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "finds all recovery processes for vaults recoverable by the current user")
	@APIResponse(responseCode = "200")
	@Transactional
	public List<RecoveryProcessDto> findAllForRecoverableVaults() {
		var currentUser = jwt.getSubject();
		return recoverProcessRepo.findByCouncilMemberOrProcessMember(currentUser).map(RecoveryProcessDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@VaultRole(value = {}, bypassForEmergencyAccess = true)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "finds an existing recovery process")
	@APIResponse(responseCode = "200")
	@Transactional
	public List<RecoveryProcessDto> findByVaultId(@PathParam("vaultId") UUID vaultId) {
		return recoverProcessRepo.findByVaultId(vaultId).map(RecoveryProcessDto::fromEntity).toList();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RecoveryProcessDto(
			@JsonProperty("id") @NotNull UUID id,
			@JsonProperty("vaultId") @NotNull UUID vaultId,
			@JsonProperty("type") @NotNull EmergencyRecoveryProcess.Type type,
			@JsonProperty("details") @RawJson @Nullable String details,
			@JsonProperty("requiredKeyShares") @Min(2) int requiredKeyShares,
			@JsonProperty("processPublicKey") @NotNull String processPublicKey,
			@JsonProperty("recoveredKeyShares") @NotEmpty Map<String, RecoveredKeyShareDto> recoveredKeyShares) {

		public static RecoveryProcessDto fromEntity(EmergencyRecoveryProcess entity) {
			var keyShareDtos = entity.getRecoveredKeyShares().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> RecoveredKeyShareDto.fromEntity(e.getValue())));
			return new RecoveryProcessDto(
					entity.getId(),
					entity.getVaultId(),
					entity.getType(),
					entity.getDetails(),
					entity.getRequiredKeyShares(),
					entity.getProcessPublicKey(),
					keyShareDtos
			);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RecoveredKeyShareDto(@JsonProperty("processPrivateKey") @ValidJWE String processPrivateKey, @JsonProperty("unrecoveredKeyShare") @ValidJWE String unrecoveredKeyShare,
									   @JsonProperty("recoveredKeyShare") @ValidJWE @Nullable String recoveredKeyShare, @JsonProperty("signedProcessInfo") @ValidJWS @Nullable String signedProcessInfo) {

		public static RecoveredKeyShareDto fromEntity(RecoveredEmergencyKeyShares entity) {
			return new RecoveredKeyShareDto(entity.getProcessPrivateKey(), entity.getUnrecoveredKeyShare(), entity.getRecoveredKeyShare(), entity.getSignedProcessInfo());
		}
	}

}