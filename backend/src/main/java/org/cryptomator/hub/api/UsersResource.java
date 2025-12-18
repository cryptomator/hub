package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
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
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.LegacyDevice;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.WotEntry;
import org.cryptomator.hub.entities.events.AuditEvent;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultKeyRetrievedEvent;
import org.cryptomator.hub.keycloak.KeycloakAdminService;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	private static final Logger LOG = Logger.getLogger(UsersResource.class.getName());

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
	Group.Repository groupRepo;

	@Inject
	JsonWebToken jwt;

	@Inject
	KeycloakAdminService keycloakAdminService;

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
	@Parameter(name = "withLastAccess", in = ParameterIn.QUERY, description = "adds last access values to the devices (if present)")
	@APIResponse(responseCode = "200", description = "returns the current user")
	@APIResponse(responseCode = "404", description = "no user matching the subject of the JWT passed as Bearer Token")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices, @QueryParam("withLastAccess") boolean withLastAccess) {
		User user = userRepo.findById(jwt.getSubject());
		Set<DeviceResource.DeviceDto> deviceDtos;
		if (withLastAccess) {
			var devices = user.devices.stream().collect(Collectors.toMap(Device::getId, Function.identity()));
			var events = auditEventRepo.findLastVaultKeyRetrieve(devices.keySet()).collect(Collectors.toMap(VaultKeyRetrievedEvent::getDeviceId, Function.identity()));
			deviceDtos = devices.values().stream().map(d -> {
				var event = events.get(d.getId());
				return DeviceResource.DeviceDto.fromEntity(d, event);
			}).collect(Collectors.toSet());
		} else if (withDevices) {
			deviceDtos = user.getDevices().stream().map(DeviceResource.DeviceDto::fromEntity).collect(Collectors.toSet());
		} else {
			deviceDtos = Set.of();
		}
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getLanguage(), deviceDtos, user.getEcdhPublicKey(), user.getEcdsaPublicKey(), user.getPrivateKeys(), user.getSetupCode());
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
		var legacyDevices = user.legacyDevices.stream().collect(Collectors.toMap(LegacyDevice::getId, Function.identity()));
		var events = auditEventRepo.findLastVaultKeyRetrieve(legacyDevices.keySet()).collect(Collectors.toMap(VaultKeyRetrievedEvent::getDeviceId, Function.identity()));
		var deviceDtos = legacyDevices.values().stream().map(d -> {
			var event = events.get(d.getId());
			return DeviceResource.DeviceDto.fromEntity(d, event);
		}).collect(Collectors.toSet());
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getLanguage(), deviceDtos, user.getEcdhPublicKey(), user.getEcdsaPublicKey(), user.getPrivateKeys(), user.getSetupCode());
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
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all users with counts")
	public List<UserDto.UserDtoWithCounts> getAll() {
		return userRepo.findAll().stream()
				.map(user -> UserDto.justPublicInfoWithCounts(
						user,
						userRepo.countGroupsForUser(user.getId()),
						userRepo.countVaultsForUser(user.getId()),
						userRepo.countDevicesForUser(user.getId())
				))
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
		try {
			var userRepresentation = keycloakAdminService.createUser(
					dto.username(),
					dto.email(),
					dto.firstName(),
					dto.lastName(),
					dto.password(),
					dto.pictureUrl(),
					dto.groupIds()
			);

			if (dto.roles() != null && !dto.roles().isEmpty()) {
				keycloakAdminService.updateUserRoles(userRepresentation.getId(), dto.roles());
			}

			User user = userRepo.findById(userRepresentation.getId());
			if (user == null) {
				throw new RuntimeException("User was created in Keycloak but not found in database after sync");
			}

			return Response.created(URI.create("./" + user.getId()))
					.entity(UserDto.justPublicInfo(user))
					.build();
		} catch (ClientErrorException e) {
			// Return 409 with specific error message (EMAIL_EXISTS or USERNAME_EXISTS)
			return Response.status(Response.Status.CONFLICT)
					.entity(e.getMessage())
					.type(MediaType.TEXT_PLAIN)
					.build();
		}
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
	public UserDtoWithTimestamp getUser(@PathParam("id") String userId) {
		User user = userRepo.findById(userId);
		if (user == null) {
			throw new NotFoundException("User not found: " + userId);
		}

		Long createdTimestamp = null;
		String firstName = null;
		String lastName = null;
		try {
			var keycloakUser = keycloakAdminService.getUser(userId);
			createdTimestamp = keycloakUser.getCreatedTimestamp();
			firstName = keycloakUser.getFirstName();
			lastName = keycloakUser.getLastName();
		} catch (Exception e) {
			LOG.fine("Could not fetch Keycloak user data for " + userId);
		}

		// Fetch groups for the user
		List<GroupDto> groups = userRepo.getGroupsForUser(userId)
				.map(GroupDto::fromEntity)
				.toList();

		// Fetch vaults with roles for the user
		List<VaultResource.VaultDtoWithRole> vaults = userRepo.getVaultAccessForUser(userId)
				.map(eva -> {
					Vault vault = vaultRepo.findById(eva.getId().getVaultId());
					return VaultResource.VaultDtoWithRole.from(vault, eva.getId().getRole());
				})
				.toList();

		// Fetch devices (modern devices)
		Set<DeviceResource.DeviceDto> devices = user.devices.stream()
				.map(DeviceResource.DeviceDto::fromEntity)
				.collect(Collectors.toSet());

		// Fetch legacy devices
		Set<DeviceResource.DeviceDto> legacyDevices = user.legacyDevices.stream()
				.map(DeviceResource.DeviceDto::fromEntity)
				.collect(Collectors.toSet());

		// Fetch roles
		Set<String> roles = keycloakAdminService.getUserRoles(userId);

		return UserDtoWithTimestamp.from(
				UserDto.justPublicInfo(user),
				createdTimestamp,
				firstName,
				lastName,
				groups,
				vaults,
				devices,
				legacyDevices,
				roles
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
	public UserDto updateUser(@PathParam("id") String userId, @Valid @NotNull UpdateUserDto dto) {
		try {
			keycloakAdminService.updateUser(
					userId,
					dto.firstName(),
					dto.lastName(),
					dto.password(),
					dto.pictureUrl()
			);

			if (dto.roles() != null) {
				keycloakAdminService.updateUserRoles(userId, dto.roles());
			}

			User user = userRepo.findById(userId);
			if (user == null) {
				throw new NotFoundException("User not found after update: " + userId);
			}

			return UserDto.justPublicInfo(user);
		} catch (ForbiddenException | NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to update user", e);
		}
	}

	@DELETE
	@Path("/{id}")
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "delete a user from Keycloak")
	@APIResponse(responseCode = "204", description = "user deleted")
	@APIResponse(responseCode = "403", description = "user has federated identity and cannot be deleted")
	@APIResponse(responseCode = "404", description = "user not found")
	public Response deleteUser(@PathParam("id") String userId) {
		try {
			keycloakAdminService.deleteUser(userId);
			return Response.noContent().build();
		} catch (ForbiddenException | NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete user", e);
		}
	}

	public record CreateUserDto(
			@JsonProperty("username") @NotNull String username,
			@JsonProperty("email") @NotNull String email,
			@JsonProperty("firstName") @NotNull String firstName,
			@JsonProperty("lastName") @NotNull String lastName,
			@JsonProperty("password") @NotNull String password,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("groupIds") Set<String> groupIds,
			@JsonProperty("roles") Set<String> roles
	) {
	}

	public record UpdateUserDto(
			@JsonProperty("firstName") String firstName,
			@JsonProperty("lastName") String lastName,
			@JsonProperty("password") String password,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("roles") Set<String> roles
	) {
	}

	public record UserDtoWithTimestamp(
			@JsonProperty("id") String id,
			@JsonProperty("type") AuthorityDto.Type type,
			@JsonProperty("name") String name,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("email") String email,
			@JsonProperty("language") String language,
			@JsonProperty("ecdhPublicKey") String ecdhPublicKey,
			@JsonProperty("ecdsaPublicKey") String ecdsaPublicKey,
			@JsonProperty("createdTimestamp") Long createdTimestamp,
			@JsonProperty("firstName") String firstName,
			@JsonProperty("lastName") String lastName,
			@JsonProperty("groups") List<GroupDto> groups,
			@JsonProperty("vaults") List<VaultResource.VaultDtoWithRole> vaults,
			@JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			@JsonProperty("legacyDevices") Set<DeviceResource.DeviceDto> legacyDevices,
			@JsonProperty("roles") Set<String> roles
	) {
		public static UserDtoWithTimestamp from(UserDto userDto, Long createdTimestamp, String firstName, String lastName, List<GroupDto> groups, List<VaultResource.VaultDtoWithRole> vaults, Set<DeviceResource.DeviceDto> devices, Set<DeviceResource.DeviceDto> legacyDevices, Set<String> roles) {
			return new UserDtoWithTimestamp(
					userDto.id,
					userDto.type,
					userDto.name,
					userDto.pictureUrl,
					userDto.getEmail(),
					userDto.getLanguage(),
					userDto.getEcdhPublicKey(),
					userDto.getEcdsaPublicKey(),
					createdTimestamp,
					firstName,
					lastName,
					groups,
					vaults,
					devices,
					legacyDevices,
					roles
			);
		}
	}

}