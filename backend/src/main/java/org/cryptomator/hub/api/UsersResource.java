package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.cryptomator.hub.entities.EmergencyRecoveryProcess;
import org.cryptomator.hub.entities.LegacyDevice;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.WotEntry;
import org.cryptomator.hub.entities.events.AuditEvent;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultKeyRetrievedEvent;
import org.cryptomator.hub.keycloak.KeycloakAuthorityPuller;
import org.cryptomator.hub.keycloak.RealmRole;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
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

	private final AccessToken.Repository accessTokenRepo;
	private final EventLogger eventLogger;
	private final User.Repository userRepo;
	private final Device.Repository deviceRepo;
	private final Vault.Repository vaultRepo;
	private final EmergencyRecoveryProcess.Repository emergencyRecovery;
	private final WotEntry.Repository wotRepo;
	private final EffectiveWot.Repository effectiveWotRepo;
	private final AuditEvent.Repository auditEventRepo;
	private final JsonWebToken jwt;
	private final KeycloakAuthorityPuller keycloakAuthorityPuller;

	@Inject
	UsersResource(AccessToken.Repository accessTokenRepo, EventLogger eventLogger, User.Repository userRepo, Device.Repository deviceRepo, Vault.Repository vaultRepo, EmergencyRecoveryProcess.Repository emergencyRecovery, WotEntry.Repository wotRepo, EffectiveWot.Repository effectiveWotRepo, AuditEvent.Repository auditEventRepo, JsonWebToken jwt, KeycloakAuthorityPuller keycloakAuthorityPuller) {
		this.accessTokenRepo = accessTokenRepo;
		this.eventLogger = eventLogger;
		this.userRepo = userRepo;
		this.deviceRepo = deviceRepo;
		this.vaultRepo = vaultRepo;
		this.emergencyRecovery = emergencyRecovery;
		this.wotRepo = wotRepo;
		this.effectiveWotRepo = effectiveWotRepo;
		this.auditEventRepo = auditEventRepo;
		this.jwt = jwt;
		this.keycloakAuthorityPuller = keycloakAuthorityPuller;
	}

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
			var devices = userEntity.getDevices().stream().collect(Collectors.toUnmodifiableMap(Device::getId, Function.identity()));
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
			eventLogger.logVaultAccessGranted(user.getId(), vault.getId(), user.getId(), false);
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
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices) {
		User user = userRepo.findById(jwt.getSubject());
		Set<DeviceResource.DeviceDto> deviceDtos;
		if (withDevices) {
			deviceDtos = user.getDevices().stream().map(DeviceResource.DeviceDto::fromEntity).collect(Collectors.toSet());
		} else {
			deviceDtos = Set.of();
		}
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getLanguage(), user.isEnabled(), deviceDtos, user.getEcdhPublicKey(), user.getEcdsaPublicKey(), user.getPrivateKeys(), user.getSetupCode());
	}

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	@GET
	@Path("/me-with-legacy-devices-and-access")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "200", description = "returns the current user")
	@APIResponse(responseCode = "404", description = "no user matching the subject of the JWT passed as Bearer Token")
	public UserDto getMeWithLegacyDevicesAndAccess() {
		User user = userRepo.findById(jwt.getSubject());
		var deviceDtos = legacyDevicesWithLastAccess(user);
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getLanguage(), user.isEnabled(), deviceDtos, user.getEcdhPublicKey(), user.getEcdsaPublicKey(), user.getPrivateKeys(), user.getSetupCode());
	}

	/**
	 * Loads a user's legacy devices with their last access values derived from audit events.
	 *
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	private Set<DeviceResource.DeviceDto> legacyDevicesWithLastAccess(User user) {
		var legacyDevices = user.getLegacyDevices().stream().collect(Collectors.toMap(LegacyDevice::getId, Function.identity()));
		var events = auditEventRepo.findLastVaultKeyRetrieve(legacyDevices.keySet()).collect(Collectors.toMap(VaultKeyRetrievedEvent::getDeviceId, Function.identity()));
		return legacyDevices.values().stream()
				.map(d -> DeviceResource.DeviceDto.fromEntity(d, events.get(d.getId())))
				.collect(Collectors.toSet());
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
		vaultRepo.deleteEmergencyKeySharesForUser(user.getId());
		emergencyRecovery.deleteKeySharesForCouncilMember(user.getId());
		deviceRepo.deleteByOwner(user.getId());
		accessTokenRepo.deleteByUser(user.getId());
		eventLogger.logUserAccountReset(jwt.getSubject());
		return Response.noContent().build();
	}

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all users with counts")
	public List<UserDto.WithCounts> getAll() {
		return userRepo.findAllWithMetrics().stream()
				.map(user -> UserDto.justPublicInfo(user).withCounts(user.metrics.getDirectGroupMembershipCount(), user.metrics.getEffectiveVaultAccessCount(), user.metrics.getDeviceCount()))
				.toList();
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
		var id = new WotEntry.Id(userId, signer.getId());
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
			return new TrustedUserDto(entity.getId().trustedUserId(), List.of(entity.getSignatureChain()));
		}
	}

	@POST
	@Path("/")
	@RolesAllowed("admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "create a new user in Keycloak")
	@APIResponse(responseCode = "201", description = "user created")
	@APIResponse(responseCode = "400", description = "invalid input")
	@APIResponse(responseCode = "409", description = "user already exists")
	public Response createUser(@Valid @NotNull CreateUserDto dto) {
		var userRepresentation = keycloakAuthorityPuller.createUser(
				dto.name(),
				dto.email(),
				dto.firstName(),
				dto.lastName(),
				dto.password(),
				dto.pictureUrl()
		);

		if (!dto.realmRoles().isEmpty()) {
			keycloakAuthorityPuller.updateUserRoles(userRepresentation.getId(), dto.realmRoles());
		}

		User user = userRepo.findByIdOptional(userRepresentation.getId()).orElseThrow(IllegalStateException::new); // user was created in Keycloak but not found in database after sync

		return Response.status(Response.Status.CREATED)
				.entity(UserDto.justPublicInfo(user))
				.build();
	}

	@GET
	@Path("/{id}")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get a specific user")
	@APIResponse(responseCode = "200", description = "user found")
	@APIResponse(responseCode = "404", description = "user not found")
	public UserDto.WithDetails getUser(@PathParam("id") String userId) {
		User user = userRepo.findByIdWithEagerDetails(userId);
		if (user == null) {
			throw new NotFoundException();
		}


		// Fetch groups for the user
		List<GroupDto> groups = user.getDirectGroupMemberships().stream()
				.map(GroupDto::fromEntity)
				.toList();

		// Fetch vaults with roles for the user
		List<VaultResource.VaultDtoWithRole> vaults = user.getAccessibleVaults().stream()
				.map(eva -> VaultResource.VaultDtoWithRole.from(eva.getVault(), eva.getRole()))
				.toList();

		// Fetch devices (modern devices)
		Set<DeviceResource.DeviceDto> devices = user.getDevices().stream()
				.map(DeviceResource.DeviceDto::fromEntity)
				.collect(Collectors.toSet());

		// Fetch legacy devices with last access (audit-derived, see #333)
		var legacyDevices = legacyDevicesWithLastAccess(user);

		// realm roles are not persisted in the Hub DB; the detailed view always reads them through from Keycloak:
		var realmRoles = keycloakAuthorityPuller.realmRolesOf(userId);

		return UserDto.justPublicInfo(user).withDetails(
				groups,
				vaults,
				devices,
				legacyDevices,
				realmRoles
		);
	}

	@PUT
	@Path("/{id}")
	@RolesAllowed("admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "update a user in Keycloak")
	@APIResponse(responseCode = "200", description = "user updated")
	@APIResponse(responseCode = "403", description = "user has federated identity and cannot be modified")
	@APIResponse(responseCode = "404", description = "user not found")
	@APIResponse(responseCode = "409", description = "email already exists")
	public UserDto updateUser(@PathParam("id") String userId, @Valid @NotNull UpdateUserDto dto) {
		keycloakAuthorityPuller.updateUser(
				userId,
				dto.email(),
				dto.firstName(),
				dto.lastName(),
				dto.password(),
				dto.pictureUrl()
		);

		keycloakAuthorityPuller.updateUserRoles(userId, dto.realmRoles());

		User user = userRepo.findById(userId);
		if (user == null) {
			throw new NotFoundException();
		}

		return UserDto.justPublicInfo(user);
	}

	@PUT
	@Path("/{id}/enabled")
	@RolesAllowed("admin")
	@Consumes(MediaType.TEXT_PLAIN)
	@Transactional
	@Operation(summary = "enable or disable a user")
	@APIResponse(responseCode = "204", description = "user updated")
	@APIResponse(responseCode = "409", description = "user attempted to disable their own account")
	public Response setUserEnabled(@PathParam("id") String userId, boolean enabled) {
		if (!enabled && Objects.equals(userId, jwt.getSubject())) {
			throw new ClientErrorException("Users cannot disable their own account", Response.Status.CONFLICT);
		}
		keycloakAuthorityPuller.setUserEnabled(userId, enabled);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{id}")
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "delete a user from Keycloak")
	@APIResponse(responseCode = "204", description = "user deleted")
	@APIResponse(responseCode = "403", description = "user has federated identity and cannot be deleted")
	@APIResponse(responseCode = "404", description = "user not found")
	@APIResponse(responseCode = "409", description = "user attempted to delete their own account")
	public Response deleteUser(@PathParam("id") String userId) {
		if (Objects.equals(userId, jwt.getSubject())) {
			throw new ClientErrorException("Users cannot delete their own account", Response.Status.CONFLICT);
		}
		keycloakAuthorityPuller.deleteUser(userId);
		return Response.noContent().build();
	}

	public record CreateUserDto(
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("email") @NotNull String email,
			@JsonProperty("firstName") @NotNull String firstName,
			@JsonProperty("lastName") @NotNull String lastName,
			@JsonProperty("password") @NotNull String password,
			@JsonProperty("pictureUrl") @Size(max = 255) @Nullable String pictureUrl,
			@JsonProperty("realmRoles") @NotNull Set<RealmRole> realmRoles
	) {
	}

	public record UpdateUserDto(
			@JsonProperty("email") @Nullable String email,
			@JsonProperty("firstName") @Nullable String firstName,
			@JsonProperty("lastName") @Nullable String lastName,
			@JsonProperty("password") @Nullable String password,
			@JsonProperty("pictureUrl") @Size(max = 255) @Nullable String pictureUrl,
			@JsonProperty("realmRoles") @NotNull Set<RealmRole> realmRoles
	) {
	}

}