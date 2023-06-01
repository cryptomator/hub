package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.LegacyAccessToken;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.filters.ActiveLicense;
import org.cryptomator.hub.filters.VaultAdminOnlyFilter;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.validation.NoHtmlOrScriptChars;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidId;
import org.cryptomator.hub.validation.ValidJWE;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Path("/vaults")
public class VaultResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	SecurityIdentity identity;

	@Inject
	LicenseHolder license;

	@GET
	@Path("/accessible")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults that have been shared with the currently logged in user or a group in wich this user is")
	public List<VaultDto> getAccessible(@Nullable @QueryParam("role") VaultAccess.Role role) {
		var currentUserId = jwt.getSubject();
		// TODO refactor to JEP 441 in JDK 21
		final Stream<Vault> resultStream;
		if (role == null) {
			resultStream = Vault.findAccessibleByUser(currentUserId);
		} else {
			resultStream = Vault.findAccessibleByUser(currentUserId, role);
		}
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/all")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all vaults", description = "list all vaults in the system")
	public List<VaultDto> getAllVaults() {
		return Vault.findAll().<Vault>stream().map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/members")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list vault members", description = "list all users that this vault has been shared with")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<AuthorityDto> getMembers(@PathParam("vaultId") UUID vaultId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);

		return vault.directMembers.stream().map(authority -> {
			if (authority instanceof User u) {
				return UserDto.justPublicInfo(u);
			} else if (authority instanceof Group g) {
				return GroupDto.fromEntity(g);
			} else {
				throw new IllegalStateException();
			}
		}).toList();
	}

	@PUT
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a member to this vault")
	@APIResponse(responseCode = "201", description = "member added")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "402", description = "all seats in license used")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault or user not found")
	@ActiveLicense
	public Response addUser(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		if (!EffectiveVaultAccess.isUserOccupyingSeat(userId)) {
			//for new user, we need to check if a license seat is available
			var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
			if (usedSeats >= license.getAvailableSeats()) {
				throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
			}
		}

		var id = new VaultAccess.Id(vaultId, userId);
		var access = VaultAccess.<VaultAccess>findById(id);
		if (access == null) {
			access = new VaultAccess();
			access.vault = vault;
			access.authority = user;
		}
		access.role = role;
		access.persistAndFlush();
		return Response.status(Response.Status.CREATED).build();
	}

	@PUT
	@Path("/{vaultId}/groups/{groupId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a group to this vault")
	@APIResponse(responseCode = "201", description = "member added")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "402", description = "used seats + (number of users in group not occupying a seats) exceeds number of total avaible seats in license")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault or group not found")
	@ActiveLicense
	public Response addGroup(@PathParam("vaultId") UUID vaultId, @PathParam("groupId") @ValidId String groupId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		//usersInGroup - usersInGroupAndPartOfAtLeastOneVault + usersOfAtLeastOneVault
		if (EffectiveGroupMembership.countEffectiveGroupUsers(groupId) - EffectiveVaultAccess.countEffectiveVaultUsersOfGroup(groupId) + EffectiveVaultAccess.countEffectiveVaultUsers() > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
		}

		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var group = Group.<Group>findByIdOptional(groupId).orElseThrow(NotFoundException::new);

		var id = new VaultAccess.Id(vaultId, groupId);
		var access = VaultAccess.<VaultAccess>findById(id);
		if (access == null) {
			access = new VaultAccess();
			access.vault = vault;
			access.authority = group;
		}
		access.role = role;
		access.persistAndFlush();
		return Response.status(Response.Status.CREATED).build();
	}

	@DELETE
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a member from this vault", description = "revokes the given user's access rights from this vault. If the given user is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "member removed")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public Response removeMember(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId) {
		return removeAuthority(vaultId, userId);
	}

	@DELETE
	@Path("/{vaultId}/groups/{groupId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a group from this vault", description = "revokes the given group's access rights from this vault. If the given group is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "member removed")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public Response removeGroup(@PathParam("vaultId") UUID vaultId, @PathParam("groupId") @ValidId String groupId) {
		return removeAuthority(vaultId, groupId);
	}

	private Response removeAuthority(UUID vaultId, String authorityId) {
		if (VaultAccess.deleteById(new VaultAccess.Id(vaultId, authorityId))) {
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			throw new NotFoundException();
		}
	}

	@GET
	@Path("/{vaultId}/users-requiring-access-grant")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<UserDto> getUsersRequiringAccessGrant(@PathParam("vaultId") UUID vaultId) {
		return User.findRequiringAccessGrant(vaultId).map(UserDto::justPublicInfo).toList();
	}

	@Deprecated(forRemoval = true)
	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the device-specific masterkey", deprecated = true)
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "number of effective vault users exceeds available license seats")
	@APIResponse(responseCode = "403", description = "device not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "unknown device")
	@ActiveLicense
	public String legacyUnlock(@PathParam("vaultId") UUID vaultId, @PathParam("deviceId") @ValidId String deviceId) {
		var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var access = LegacyAccessToken.unlock(vaultId, deviceId, jwt.getSubject());
		if (access != null) {
			return access.jwe;
		} else if (Device.findById(deviceId) == null) {
			throw new NotFoundException("No such device.");
		} else {
			throw new ForbiddenException("Access to this device not granted.");
		}
	}

	@GET
	@Path("/{vaultId}/user-tokens/me")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the user-specific vault key", description = "retrieves the vault jwe for the current user")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "number of effective vault users exceeds available license seats")
	@APIResponse(responseCode = "403", description = "user not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "unknown vault")
	@ActiveLicense
	public String unlock(@PathParam("vaultId") UUID vaultId) {
		var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var access = AccessToken.unlock(vaultId, jwt.getSubject());
		if (access != null) {
			return access.jwe;
		} else if (Vault.findById(vaultId) == null) {
			throw new NotFoundException("No such vault.");
		} else {
			throw new ForbiddenException("Access to this vault not granted.");
		}
	}

	@PUT
	@Path("/{vaultId}/user-tokens/{userId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "adds a user-specific vault key")
	@APIResponse(responseCode = "201", description = "user-specific key stored")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault or userId not found")
	@APIResponse(responseCode = "409", description = "Access to vault for device already granted")
	public Response grantAccess(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId, @NotNull @ValidJWE String jwe) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new); // should always be found, since @VaultAdminOnlyFilter already checked the jwt matching this vault
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);

		var access = new AccessToken();
		access.vault = vault;
		access.user = user;
		access.jwe = jwe;

		try {
			access.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			throw new ClientErrorException(Response.Status.CONFLICT, e);
		}
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a vault")
	@APIResponse(responseCode = "403", description = "requesting user is not member of the vault")
	public VaultDto get(@PathParam("vaultId") UUID vaultId) {
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.effectiveMembers.stream().noneMatch(u -> u.id.equals(jwt.getSubject())) && !identity.getRoles().contains("admin")) {
			throw new ForbiddenException("Requesting user is not a member of the vault");
		}
		return VaultDto.fromEntity(vault);
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates a vault",
			description = "Creates a vault with the given vault id, owned by the logged in user. The creationTime in the vaultDto is ignored and the current server time is used.")
	@APIResponse(responseCode = "201", description = "vault created")
	@APIResponse(responseCode = "409", description = "vault with given id or name already exists")
	public Response create(@PathParam("vaultId") UUID vaultId, @Valid VaultDto vaultDto) {
		if (vaultDto == null) {
			throw new BadRequestException("Missing vault dto");
		}
		User currentUser = User.findById(jwt.getSubject());
		var vault = vaultDto.toVault(vaultId);
		vault.creationTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		var access = new VaultAccess();
		access.vault = vault;
		access.authority = currentUser;
		access.role = VaultAccess.Role.OWNER;
		try {
			vault.persistAndFlush();
			access.persist();
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			throw new ClientErrorException(Response.Status.CONFLICT, e);
		}
	}

	public record VaultDto(@JsonProperty("id") UUID id,
						   @JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
						   @JsonProperty("description") @NoHtmlOrScriptChars String description,
						   @JsonProperty("creationTime") Instant creationTime,
						   @JsonProperty("masterkey") @NotNull @OnlyBase64Chars String masterkey, @JsonProperty("iterations") int iterations,
						   @JsonProperty("salt") @NotNull @OnlyBase64Chars String salt,
						   @JsonProperty("authPublicKey") @NotNull @OnlyBase64Chars String authPublicKey, @JsonProperty("authPrivateKey") @NotNull @OnlyBase64Chars String authPrivateKey
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.description, entity.creationTime.truncatedTo(ChronoUnit.MILLIS), entity.masterkey, entity.iterations, entity.salt, entity.authenticationPublicKey, entity.authenticationPrivateKey);
		}

		public Vault toVault(UUID id) {
			var vault = new Vault();
			vault.id = id;
			vault.name = name;
			vault.description = description;
			vault.creationTime = creationTime;
			vault.masterkey = masterkey;
			vault.iterations = iterations;
			vault.salt = salt;
			vault.authenticationPublicKey = authPublicKey;
			vault.authenticationPrivateKey = authPrivateKey;
			return vault;
		}

	}
}
