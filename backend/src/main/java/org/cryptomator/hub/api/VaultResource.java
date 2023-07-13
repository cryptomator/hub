package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.CreateVaultEvent;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.UnlockVaultEvent;
import org.cryptomator.hub.entities.UpdateVaultEvent;
import org.cryptomator.hub.entities.UpdateVaultMembershipEvent;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
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
import java.util.NoSuchElementException;
import java.util.UUID;

@Path("/vaults")
public class VaultResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	SecurityIdentity identity;

	@Inject
	LicenseHolder license;

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults that are not archived and have been shared with the currently logged in user or a group this user is part of")
	public List<VaultDto> getAccessible() {
		var currentUserId = jwt.getSubject();
		var resultStream = Vault.findAccessibleByUser(currentUserId);
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/some")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all vaults corresponding to the given ids", description = "list for each id in the list its corresponding vault. Ignores all id's where a vault does not exist, ")
	@APIResponse(responseCode = "200")
	public List<VaultDto> getSomeVaults(@QueryParam("ids") List<UUID> vaultIds) {
		return Vault.findAllInList(vaultIds).map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/all")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults in the system")
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
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<AuthorityDto> getMembers(@PathParam("vaultId") UUID vaultId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);

		return vault.directMembers.stream().map(authority -> {
			if (authority instanceof User u) {
				return UserDto.fromEntity(u);
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
	@APIResponse(responseCode = "409", description = "user is already a direct member of the vault")
	@ActiveLicense
	public Response addUser(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		if (!EffectiveVaultAccess.isUserOccupyingSeat(userId)) {
			//for new user, we need to check if a license seat is available
			var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
			if (usedSeats >= license.getAvailableSeats()) {
				throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
			}
		}
		if (vault.directMembers.contains(user)) {
			return Response.status(Response.Status.CONFLICT).build();
		}

		vault.directMembers.add(user);
		vault.persist();
		UpdateVaultMembershipEvent.log(jwt.getSubject(), vaultId, userId, UpdateVaultMembershipEvent.Operation.ADD);
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
	@APIResponse(responseCode = "409", description = "group is already a direct member of the vault")
	@ActiveLicense
	public Response addGroup(@PathParam("vaultId") UUID vaultId, @PathParam("groupId") @ValidId String groupId) {
		//usersInGroup - usersInGroupAndPartOfAtLeastOneVault + usersOfAtLeastOneVault
		if (EffectiveGroupMembership.countEffectiveGroupUsers(groupId) - EffectiveVaultAccess.countEffectiveVaultUsersOfGroup(groupId) + EffectiveVaultAccess.countEffectiveVaultUsers() > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
		}

		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var group = Group.<Group>findByIdOptional(groupId).orElseThrow(NotFoundException::new);
		if (vault.directMembers.contains(group)) {
			return Response.status(Response.Status.CONFLICT).build();
		}
		vault.directMembers.add(group);
		vault.persist();
		UpdateVaultMembershipEvent.log(jwt.getSubject(), vaultId, groupId, UpdateVaultMembershipEvent.Operation.ADD);
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
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		vault.directMembers.removeIf(e -> e.id.equals(authorityId));
		vault.persist();
		UpdateVaultMembershipEvent.log(jwt.getSubject(), vaultId, authorityId, UpdateVaultMembershipEvent.Operation.REMOVE);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@Path("/{vaultId}/devices-requiring-access-grant")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<DeviceResource.DeviceDto> getDevicesRequiringAccessGrant(@PathParam("vaultId") UUID vaultId) {
		return Device.findRequiringAccessGrant(vaultId).map(DeviceResource.DeviceDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the device-specific masterkey of a non-archived vault")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "number of effective vault users exceeds available license seats")
	@APIResponse(responseCode = "403", description = "device not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "vault or device not found")
	@APIResponse(responseCode = "410", description = "Vault is archived")
	@ActiveLicense
	public Response unlock(@PathParam("vaultId") UUID vaultId, @PathParam("deviceId") @ValidId String deviceId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.archived) {
			throw new GoneException("Vault is archived.");
		}

		var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var access = AccessToken.unlock(vaultId, deviceId, jwt.getSubject());
		if (access != null) {
			UnlockVaultEvent.log(jwt.getSubject(), vaultId, deviceId, UnlockVaultEvent.Result.SUCCESS);
			var subscriptionStateHeaderName = "Hub-Subscription-State";
			var subscriptionStateHeaderValue = license.isSet() ? "ACTIVE" : "INACTIVE"; // license expiration is not checked here, because it is checked in the ActiveLicense filter
			return Response.ok(access.jwe).header(subscriptionStateHeaderName, subscriptionStateHeaderValue).build();
		} else if (Device.findById(deviceId) == null) {
			throw new NotFoundException("No such device.");
		} else {
			UnlockVaultEvent.log(jwt.getSubject(), vaultId, deviceId, UnlockVaultEvent.Result.UNAUTHORIZED);
			throw new ForbiddenException("Access to this device not granted.");
		}
	}

	@PUT
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "adds a device-specific masterkey to a non-archived vault")
	@APIResponse(responseCode = "201", description = "device-specific key stored")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault or device not found")
	@APIResponse(responseCode = "409", description = "Access to vault for device already granted")
	@APIResponse(responseCode = "410", description = "Vault is archived")
	public Response grantAccess(@PathParam("vaultId") UUID vaultId, @PathParam("deviceId") @ValidId String deviceId, @ValidJWE String jwe) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.archived) {
			throw new GoneException("Vault is archived.");
		}
		var device = Device.<Device>findByIdOptional(deviceId).orElseThrow(NotFoundException::new);

		var access = new AccessToken();
		access.vault = vault;
		access.device = device;
		access.jwe = jwe;

		try {
			access.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (PersistenceException e) {
			if (e instanceof ConstraintViolationException) {
				throw new ClientErrorException(Response.Status.CONFLICT, e);
			} else {
				throw new InternalServerErrorException(e);
			}
		}
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a vault")
	@APIResponse(responseCode = "200")
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
	@Operation(summary = "creates or updates a vault",
			description = "Creates or updates a vault with the given vault id. The creationTime in the vaultDto is always ignored. On creation the current server time is used. On update, only the name, description and archived fields are considered.")
	@APIResponse(responseCode = "200", description = "existing vault updated")
	@APIResponse(responseCode = "201", description = "new vault created")
	public Response createOrUpdate(@PathParam("vaultId") UUID vaultId, @Valid @NotNull VaultDto vaultDto) {
		var currentUser = User.<User>findById(jwt.getSubject());
		Vault vault;
		boolean isCreated = false;
		try {
			vault = Vault.<Vault>findByIdOptional(vaultId).get();
		} catch (NoSuchElementException _e) {
			isCreated = true;
			//create new vault
			vault = new Vault();
			vault.id = vaultDto.id;
			vault.creationTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
			vault.masterkey = vaultDto.masterkey;
			vault.iterations = vaultDto.iterations;
			vault.salt = vaultDto.salt;
			vault.authenticationPublicKey = vaultDto.authPublicKey;
			vault.authenticationPrivateKey = vaultDto.authPrivateKey;
			vault.directMembers.add(currentUser);
		}
		//update new or existing vault
		vault.name = vaultDto.name;
		vault.description = vaultDto.description;
		vault.archived = isCreated ? false : vaultDto.archived;
		vault.persistAndFlush();
		if (isCreated) {
			CreateVaultEvent.log(currentUser.id, vault.id, vault.name, vault.description);
			UpdateVaultMembershipEvent.log(currentUser.id, vaultId, currentUser.id, UpdateVaultMembershipEvent.Operation.ADD);
			return Response.created(URI.create(".")).contentLocation(URI.create(".")).entity(VaultDto.fromEntity(vault)).type(MediaType.APPLICATION_JSON).build();
		} else {
			UpdateVaultEvent.log(currentUser.id, vault.id, vault.name, vault.description, vault.archived);
			return Response.ok(VaultDto.fromEntity(vault), MediaType.APPLICATION_JSON).build();
		}
	}


	public record VaultDto(@JsonProperty("id") UUID id,
						   @JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
						   @JsonProperty("description") @NoHtmlOrScriptChars String description,
						   @JsonProperty("archived") boolean archived,
						   @JsonProperty("creationTime") Instant creationTime,
						   @JsonProperty("masterkey") @OnlyBase64Chars String masterkey, @JsonProperty("iterations") int iterations,
						   @JsonProperty("salt") @OnlyBase64Chars String salt,
						   @JsonProperty("authPublicKey") @OnlyBase64Chars String authPublicKey, @JsonProperty("authPrivateKey") @OnlyBase64Chars String authPrivateKey
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.description, entity.archived, entity.creationTime.truncatedTo(ChronoUnit.MILLIS), entity.masterkey, entity.iterations, entity.salt, entity.authenticationPublicKey, entity.authenticationPrivateKey);
		}

	}
}
