package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveWot;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.WotEntry;
import org.cryptomator.hub.entities.events.AuditEvent;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultKeyRetrievedEvent;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	AccessToken.Repository accessTokenRepo;
	@Inject
	EventLogger eventLogger;
	@Inject
	User.Repository userRepo;
	@Inject
	Device.Repository deviceRepo;
	@Inject
	Vault.Repository vaultRepo;
	@Inject
	WotEntry.Repository wotRepo;
	@Inject
	EffectiveWot.Repository effectiveWotRepo;
	@Inject
	AuditEvent.Repository auditEventRepo;

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "update the logged-in user")
	@APIResponse(responseCode = "201", description = "user created or updated")
	public Response putMe(@Nullable @Valid UserDto dto) {
		var userId = jwt.getSubject();
		User user = userRepo.findById(userId);
		if (user == null) {
			user = new User();
			user.setId(userId);
		}
		user.setName(jwt.getName());
		user.setPictureUrl(jwt.getClaim("picture"));
		user.setEmail(jwt.getClaim("email"));
		if (dto != null) {
			if (!Objects.equals(user.getSetupCode(), dto.getSetupCode())) {
				user.setSetupCode(dto.getSetupCode());
				eventLogger.logUserSetupCodeChanged(jwt.getSubject());
			}
			if (!Objects.equals(user.getEcdhPublicKey(), dto.getEcdhPublicKey()) || !Objects.equals(user.getEcdsaPublicKey(), dto.getEcdsaPublicKey()) || !Objects.equals(user.getPrivateKeys(), dto.getPrivateKeys())) {
				user.setEcdhPublicKey(dto.getEcdhPublicKey());
				user.setEcdsaPublicKey(dto.getEcdsaPublicKey());
				user.setPrivateKeys(dto.getPrivateKeys());
				eventLogger.logUserKeysChanged(jwt.getSubject(), jwt.getName());
			}
			updateDevices(user, dto);
			user.setLanguage(dto.getLanguage());
		}
		userRepo.persist(user);
		return Response.created(URI.create(".")).build();
	}

	/**
	 * Updates those devices that are present in both the entity and the DTO. No devices are added or removed.
	 *
	 * @param userEntity The persistent entity
	 * @param userDto    The DTO
	 */
	private void updateDevices(User userEntity, UserDto userDto) {
		if (userDto.getDevices() != null) {
			var devices = userEntity.devices.stream().collect(Collectors.toUnmodifiableMap(Device::getId, Function.identity()));
			var updatedDevices = userDto.getDevices().stream()
					.filter(d -> devices.containsKey(d.id())) // only look at DTOs for which we find a matching existing entity
					.map(dto -> {
						var device = devices.get(dto.id());
						device.setType(dto.type());
						device.setName(dto.name());
						device.setPublickey(dto.publicKey());
						device.setUserPrivateKeys(dto.userPrivateKeys());
						return device;
					});
			deviceRepo.persist(updatedDevices);
		}
	}

	@POST
	@Path("/me/access-tokens")
	@RolesAllowed("user")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds/updates user-specific vault keys", description = "Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).")
	@APIResponse(responseCode = "200", description = "all keys stored")
	public Response updateMyAccessTokens(@NotNull Map<UUID, String> tokens) {
		var user = userRepo.findById(jwt.getSubject());
		for (var entry : tokens.entrySet()) {
			var vault = vaultRepo.findById(entry.getKey());
			if (vault == null) {
				continue; // skip
			}
			var token = accessTokenRepo.findById(new AccessToken.AccessId(user.getId(), vault.getId()));
			if (token == null) {
				token = new AccessToken();
				token.setVault(vault);
				token.setUser(user);
			}
			token.setVaultKey(entry.getValue());
			accessTokenRepo.persist(token);
			eventLogger.logVaultAccessGranted(user.getId(), vault.getId(), user.getId());
		}
		return Response.ok().build();
	}

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "200", description = "returns the current user")
	@APIResponse(responseCode = "404", description = "no user matching the subject of the JWT passed as Bearer Token")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices, @QueryParam("withLastAccess") boolean withLastAccess) {
		User user = userRepo.findById(jwt.getSubject());
		Set<DeviceResource.DeviceDto> devices;
		if (withLastAccess) {
			var deviceEntities = user.devices.stream().toList();
			var deviceIds = deviceEntities.stream().map(Device::getId).toList();
			var events = auditEventRepo.findLastVaultKeyRetrieve(deviceIds).collect(Collectors.toMap(VaultKeyRetrievedEvent::getDeviceId, Function.identity()));
			devices = deviceEntities.stream().map(d -> {
				var event = events.get(d.getId());
				var lastIpAddress = (event != null) ? event.getIpAddress() : null;
				var lastAccessTime = (event != null) ? event.getTimestamp() : null;
				return new DeviceResource.DeviceDto(d.getId(), d.getName(), d.getType(), d.getPublickey(), d.getUserPrivateKeys(), d.getOwner().getId(), d.getCreationTime().truncatedTo(ChronoUnit.MILLIS), lastIpAddress, lastAccessTime);
			}).collect(Collectors.toSet());
		} else {
			Function<Device, DeviceResource.DeviceDto> mapDevices = d -> new DeviceResource.DeviceDto(d.getId(), d.getName(), d.getType(), d.getPublickey(), d.getUserPrivateKeys(), d.getOwner().getId(), d.getCreationTime().truncatedTo(ChronoUnit.MILLIS), null, null);
			devices = withDevices ? user.devices.stream().map(mapDevices).collect(Collectors.toSet()) : Set.of();
		}
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getLanguage(), devices, user.getEcdhPublicKey(), user.getEcdsaPublicKey(), user.getPrivateKeys(), user.getSetupCode());
	}

	@POST
	@Path("/me/reset")
	@RolesAllowed("user")
	@NoCache
	@Transactional
	@Operation(summary = "resets the user account")
	@APIResponse(responseCode = "204", description = "deleted keys, devices and access permissions")
	public Response resetMe() {
		User user = userRepo.findById(jwt.getSubject());
		user.setEcdhPublicKey(null);
		user.setEcdsaPublicKey(null);
		user.setPrivateKeys(null);
		user.setSetupCode(null);
		userRepo.persist(user);
		deviceRepo.deleteByOwner(user.getId());
		accessTokenRepo.deleteByUser(user.getId());
		eventLogger.logUserAccountReset(jwt.getSubject());
		return Response.noContent().build();
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		return userRepo.findAll().stream().map(UserDto::justPublicInfo).toList();
	}

	@PUT
	@Path("/trusted/{userId}")
	@RolesAllowed("user")
	@Transactional
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "adds/updates trust", description = "Stores a signature for the given user.")
	@APIResponse(responseCode = "204", description = "signature stored")
	public Response putSignature(@PathParam("userId") String userId, @NotNull String signature) {
		var signer = userRepo.findById(jwt.getSubject());
		var id = new WotEntry.Id();
		id.setUserId(userId);
		id.setSignerId(signer.getId());
		var entry = wotRepo.findById(id);
		if (entry == null) {
			entry = new WotEntry();
			entry.setId(id);
		}
		entry.setSignature(signature);
		wotRepo.persist(entry);
		eventLogger.logWotIdSigned(userId, signer.getId(), signer.getEcdsaPublicKey(), signature);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@Path("/trusted/{userId}")
	@RolesAllowed("user")
	@NoCache
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "get trust detail for given user", description = "returns the shortest found signature chain for the given user")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "404", description = "if no sufficiently short trust chain between the invoking user and the user with the given id has been found")
	public TrustedUserDto getTrustedUser(@PathParam("userId") String trustedUserId) {
		var trustingUserId = jwt.getSubject();
		return effectiveWotRepo.findTrusted(trustingUserId, trustedUserId).singleResultOptional().map(TrustedUserDto::fromEntity).orElseThrow(NotFoundException::new);
	}

	@GET
	@Path("/trusted")
	@RolesAllowed("user")
	@NoCache
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "get trusted users", description = "returns a list of users trusted by the currently logged-in user")
	@APIResponse(responseCode = "200")
	public List<TrustedUserDto> getTrustedUsers() {
		var trustingUserId = jwt.getSubject();
		return effectiveWotRepo.findTrusted(trustingUserId).stream().map(TrustedUserDto::fromEntity).toList();
	}

	public record TrustedUserDto(@JsonProperty("trustedUserId") String trustedUserId, @JsonProperty("signatureChain") List<String> signatureChain) {

		public static TrustedUserDto fromEntity(EffectiveWot entity) {
			return new TrustedUserDto(entity.getId().getTrustedUserId(), List.of(entity.getSignatureChain()));
		}
	}
}