package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.identity.SecurityIdentity;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.filters.ActiveLicense;
import org.cryptomator.hub.filters.VaultAdminOnlyFilter;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

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
	@Operation(summary = "list all accessible vaults", description = "list all vaults that have been shared with the currently logged in user or a group in wich this user is")
	public List<VaultDto> getSharedOrOwned() {
		var currentUserId = jwt.getSubject();
		var resultStream = Vault.findAccessibleOrOwnedByUser(currentUserId);
		return resultStream.map(VaultDto::fromEntity).toList();
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
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<AuthorityDto> getMembers(@PathParam("vaultId") String vaultId) {
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
	public Response addUser(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
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
	public Response addGroup(@PathParam("vaultId") String vaultId, @PathParam("groupId") String groupId) {
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
	public Response removeMember(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		return removeAutority(vaultId, userId);
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
	public Response removeGroup(@PathParam("vaultId") String vaultId, @PathParam("groupId") String groupId) {
		return removeAutority(vaultId, groupId);
	}

	private Response removeAutority(String vaultId, String authorityId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		vault.directMembers.removeIf(e -> e.id.equals(authorityId));
		vault.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@Path("/{vaultId}/devices-requiring-access-grant")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault not found")
	public List<DeviceResource.DeviceDto> getDevicesRequiringAccessGrant(@PathParam("vaultId") String vaultId) {
		return Device.findRequiringAccessGrant(vaultId).map(DeviceResource.DeviceDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the device-specific masterkey")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "number of effective vault users exceeds available license seats")
	@APIResponse(responseCode = "403", description = "device not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "unknown device")
	@ActiveLicense
	public String unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		var usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var access = AccessToken.unlock(vaultId, deviceId, jwt.getSubject());
		if (access != null) {
			return access.jwe;
		} else if (Device.findById(deviceId) == null) {
			throw new NotFoundException("No such device.");
		} else {
			throw new ForbiddenException("Access to this device not granted.");
		}
	}

	@PUT
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@VaultAdminOnlyFilter
	@Transactional
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "adds a device-specific masterkey")
	@APIResponse(responseCode = "201", description = "device-specific key stored")
	@APIResponse(responseCode = "401", description = "VaultAdminAuthorizationJWT not provided")
	@APIResponse(responseCode = "403", description = "VaultAdminAuthorizationJWT expired or not yet valid")
	@APIResponse(responseCode = "404", description = "vault or device not found")
	@APIResponse(responseCode = "409", description = "Access to vault for device already granted")
	public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, String jwe) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var device = Device.<Device>findByIdOptional(deviceId).orElseThrow(NotFoundException::new);

		var access = new AccessToken();
		access.vault = vault;
		access.device = device;
		access.jwe = jwe;

		try {
			access.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (PersistenceException e) {
			if (e.getCause() instanceof ConstraintViolationException) {
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
	@APIResponse(responseCode = "403", description = "requesting user is not member of the vault")
	public VaultDto get(@PathParam("vaultId") String vaultId) {
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
			description = "Creates a vault with the given vault id. The creationTime in the vaultDto is ignored and the current server time is used.")
	@APIResponse(responseCode = "201", description = "vault created")
	@APIResponse(responseCode = "409", description = "vault with given id or name already exists")
	public Response create(@PathParam("vaultId") String vaultId, VaultDto vaultDto) {
		if (vaultDto == null) {
			throw new BadRequestException("Missing vault dto");
		}
		User currentUser = User.findById(jwt.getSubject());
		var vault = vaultDto.toVault(vaultId);
		vault.directMembers.add(currentUser);
		try {
			vault.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (PersistenceException e) {
			if (e.getCause() instanceof ConstraintViolationException) {
				throw new ClientErrorException(Response.Status.CONFLICT, e);
			} else {
				throw new InternalServerErrorException(e);
			}
		}
	}

	public record VaultDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("description") String description,
						   @JsonProperty("creationTime") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Timestamp creationTime,
						   @JsonProperty("masterkey") String masterkey, @JsonProperty("iterations") String iterations, @JsonProperty("salt") String salt,
						   @JsonProperty("authPublicKey") String authPublicKey, @JsonProperty("authPrivateKey") String authPrivateKey
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.description, entity.creationTime, entity.masterkey, entity.iterations, entity.salt, entity.authenticationPublicKey, entity.authenticationPrivateKey);
		}

		public Vault toVault(String id) {
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
