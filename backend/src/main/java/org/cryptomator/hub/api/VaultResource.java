package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.license.SeatsRestricted;
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
	@Path("/{vaultId}/members")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list vault members", description = "list all users that this vault has been shared with")
	@APIResponse(responseCode = "403", description = "requesting user does not own vault")
	public List<AuthorityDto> getMembers(@PathParam("vaultId") String vaultId) {
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject())) {
			throw new ForbiddenException("Requesting user does not own vault");
		}
		return vault.directMembers.stream().map(authority -> {
			// TODO replace with pattern matching for switch as soon as available
			if (authority instanceof User u) {
				return UsersResource.UserDto.fromEntity(u);
			} else if (authority instanceof Group g) {
				return GroupResource.GroupDto.fromEntity(g);
			} else {
				throw new IllegalStateException();
			}
		}).toList();
	}

	@PUT
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a member to this vault")
	@APIResponse(responseCode = "201", description = "member added")
	@APIResponse(responseCode = "403", description = "requesting user does not own vault")
	@APIResponse(responseCode = "404", description = "vault or user not found")
	@SeatsRestricted
	public Response addUser(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject())) {
			throw new ForbiddenException("Requesting user does not own vault");
		}

		vault.directMembers.add(user);
		vault.persist();
		return Response.status(Response.Status.CREATED).build();
	}

	@PUT
	@Path("/{vaultId}/groups/{groupId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a group to this vault")
	@APIResponse(responseCode = "201", description = "member added")
	@APIResponse(responseCode = "402", description = "previously used seats + group size exceeds number of total avaible seats in license")
	@APIResponse(responseCode = "404", description = "vault or group not found")
	@SeatsRestricted
	public Response addGroup(@PathParam("vaultId") String vaultId, @PathParam("groupId") String groupId) {
		//usersInGroup - usersInGroupAndPartOfAtLeastOneVault + usersOfAtLeastOneVault
		if (EffectiveGroupMembership.countEffectiveGroupUsers(groupId) - EffectiveVaultAccess.countEffectiveVaultUsersOfGroup(groupId) + EffectiveVaultAccess.countEffectiveVaultUsers() > license.getAvailableSeats()) {
			return Response.status(Response.Status.PAYMENT_REQUIRED).build();
		}

		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var group = Group.<Group>findByIdOptional(groupId).orElseThrow(NotFoundException::new);
		vault.directMembers.add(group);
		vault.persist();
		return Response.status(Response.Status.CREATED).build();
	}

	@DELETE
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a member from this vault", description = "revokes the given user's access rights from this vault. If the given user is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "member removed")
	@APIResponse(responseCode = "403", description = "requesting user does not own vault")
	@APIResponse(responseCode = "404", description = "vault not found")
	public Response removeMember(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		return removeAutority(vaultId, userId);
	}

	@DELETE
	@Path("/{vaultId}/groups/{groupId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a group from this vault", description = "revokes the given group's access rights from this vault. If the given group is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "member removed")
	@APIResponse(responseCode = "404", description = "vault not found")
	public Response removeGroup(@PathParam("vaultId") String vaultId, @PathParam("groupId") String groupId) {
		return removeAutority(vaultId, groupId);
	}

	private Response removeAutority(String vaultId, String authorityId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject())) {
			throw new ForbiddenException("Requesting user does not own vault");
		}

		vault.directMembers.removeIf(e -> e.id.equals(authorityId));
		vault.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@Path("/{vaultId}/devices-requiring-access-grant")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
	@APIResponse(responseCode = "403", description = "requesting user does not own vault")
	public List<DeviceResource.DeviceDto> getDevicesRequiringAccessGrant(@PathParam("vaultId") String vaultId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject())) {
			throw new ForbiddenException("Requesting user does not own vault");
		}
		return Device.findRequiringAccessGrant(vaultId).map(DeviceResource.DeviceDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the device-specific masterkey")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "device not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "unknown device")
	@SeatsRestricted
	public String unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
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
	@RolesAllowed("vault-owner")
	@Transactional
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "adds a device-specific masterkey")
	@APIResponse(responseCode = "201", description = "device-specific key stored")
	@APIResponse(responseCode = "403", description = "requesting user does not own vault")
	@APIResponse(responseCode = "404", description = "specified vault or device not found")
	@APIResponse(responseCode = "409", description = "Access to vault for device already granted")
	public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, String jwe) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var device = Device.<Device>findByIdOptional(deviceId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject())) {
			throw new ForbiddenException("Requesting user does not own vault");
		}

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
	@APIResponse(responseCode = "403", description = "requesting user is neither member nor owner of the vault")
	public VaultDto get(@PathParam("vaultId") String vaultId) {
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (!vault.owner.id.equals(jwt.getSubject()) && vault.effectiveMembers.stream().noneMatch(u -> u.id.equals(jwt.getSubject()))) {
			throw new ForbiddenException("Requesting user is neither member nor owner of the vault");
		}
		return VaultDto.fromEntity(vault);
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("vault-owner")
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
		var vault = vaultDto.toVault(currentUser, vaultId);
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
						   @JsonProperty("owner") UsersResource.UserDto user,
						   @JsonProperty("masterkey") String masterkey, @JsonProperty("iterations") String iterations, @JsonProperty("salt") String salt
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.description, entity.creationTime, UsersResource.UserDto.fromEntity(entity.owner), entity.masterkey, entity.iterations, entity.salt);
		}

		public Vault toVault(User owner, String id) {
			var vault = new Vault();
			vault.id = id;
			vault.owner = owner;
			vault.name = name;
			vault.description = description;
			vault.creationTime = creationTime;
			vault.masterkey = masterkey;
			vault.iterations = iterations;
			vault.salt = salt;
			return vault;
		}

	}
}
