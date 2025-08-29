package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.cryptomator.hub.entities.events.EventLogger;
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

	@Inject
	EmergencyRecoveryProcess.Repository recoverProcessRepo;

	@Inject
	RecoveredEmergencyKeyShares.Repository recoveredKeySharesRepo;

	@Inject
	JsonWebToken jwt;

	@Context
	HttpServerRequest request;

	@Inject
	EventLogger eventLogger;

	@PUT
	@Path("/{processId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "starts a new recovery process")
	@APIResponse(responseCode = "204", description = "process created")
	@APIResponse(responseCode = "400", description = "invalid request, e.g. missing required fields")
	@Transactional
	public Response startRecovery(@PathParam("processId") UUID processId, RecoveryProcessDto dto) {
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
			keyShareEntity.getId().setRecoveryId(processId);
			keyShareEntity.getId().setCouncilMemberId(memberId);
			keyShareEntity.setProcessPrivateKey(keyShareDto.processPrivateKey);
			keyShareEntity.setUnrecoveredKeyShare(keyShareDto.unrecoveredKeyShare);
			keyShareEntity.setRecoveredKeyShare(keyShareDto.recoveredKeyShare);
			keyShareEntity.setSignedProcessInfo(keyShareDto.signedProcessInfo);
			return keyShareEntity;
		}));
		process.setRecoveredKeyShares(keyShares);
		recoverProcessRepo.persist(process);
		var currentUser = jwt.getSubject();

		// audit logging
		eventLogger.logEmergencyAccessRecoveryStarted(dto.vaultId, processId, currentUser, dto.type.name(), dto.details);
		if (keyShares.get(currentUser).getRecoveredKeyShare() != null) { // usually, the council member who starts the process also adds their key share
			eventLogger.logEmergencyAccessRecoveryApproved(processId, currentUser, request.remoteAddress().hostAddress());
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@POST
	@Path("/{processId}/recovered-key-shares")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "starts a new recovery process")
	@APIResponse(responseCode = "204", description = "process created")
	@APIResponse(responseCode = "400", description = "invalid request, e.g. missing required fields")
	@Transactional
	public Response addRecoveredKeyShare(@PathParam("processId") UUID processId, RecoveredKeyShareDto dto) {
		var id = new RecoveredEmergencyKeyShares.Id();
		id.setRecoveryId(processId);
		id.setCouncilMemberId(jwt.getSubject());

		var myKeyShare = recoveredKeySharesRepo.findById(id);

		myKeyShare.setRecoveredKeyShare(dto.recoveredKeyShare);
		myKeyShare.setSignedProcessInfo(dto.signedProcessInfo);
		recoveredKeySharesRepo.persist(myKeyShare);

		// audit logging
		eventLogger.logEmergencyAccessRecoveryApproved(processId, jwt.getSubject(), request.remoteAddress().hostAddress());

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@DELETE
	@Path("/{processId}")
	@RolesAllowed("user")
	@Operation(summary = "deletes an obsolete recovery process")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404")
	@Transactional
	public Response delete(@PathParam("processId") UUID processId) {
		if (recoverProcessRepo.deleteById(processId)) {
			return Response.noContent().build();
		} else {
			throw new NotFoundException();
		}
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "finds an existing recovery process")
	@APIResponse(responseCode = "200")
	@Transactional
	public List<RecoveryProcessDto> findByVaultId(@PathParam("vaultId") UUID vaultId) {
		return recoverProcessRepo.findByVaultId(vaultId).map(RecoveryProcessDto::fromEntity).toList();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RecoveryProcessDto(
			@JsonProperty("id") @NotBlank UUID id,
			@JsonProperty("vaultId") @NotBlank UUID vaultId,
			@JsonProperty("type") @NotBlank EmergencyRecoveryProcess.Type type,
			@JsonProperty("details") @RawJson String details,
			@JsonProperty("requiredKeyShares") @Min(2) int requiredKeyShares,
			@JsonProperty("processPublicKey") @ValidJWE String processPublicKey,
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
	public record RecoveredKeyShareDto(@JsonProperty("processPrivateKey") @ValidJWE String processPrivateKey, @JsonProperty("unrecoveredKeyShare") @ValidJWE String unrecoveredKeyShare, @JsonProperty("recoveredKeyShare") @ValidJWE String recoveredKeyShare, @JsonProperty("signedProcessInfo") @ValidJWS String signedProcessInfo) {

		public static RecoveredKeyShareDto fromEntity(RecoveredEmergencyKeyShares entity) {
			return new RecoveredKeyShareDto(entity.getProcessPrivateKey(), entity.getUnrecoveredKeyShare(), entity.getRecoveredKeyShare(), entity.getSignedProcessInfo());
		}
	}

}