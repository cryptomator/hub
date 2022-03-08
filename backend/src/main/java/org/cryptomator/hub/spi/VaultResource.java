package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.GroupAccess;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.UserAccess;
import org.cryptomator.hub.entities.Vault;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Path("/vaults")
public class VaultResource {

	@Inject
	JsonWebToken jwt;

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults that have been shared with the currently logged in user or a group in wich this user is")
	public List<VaultDto> getSharedOrOwned() {
		var currentUserId = jwt.getSubject();
		Stream<Vault> resultStream = Vault.findAccessibleOrOwnerByUser(currentUserId);
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/access")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list vault members and groups", description = "list all users and groups that this vault has been shared with")
	public VaultAccess getAccess(@PathParam("vaultId") String vaultId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		return VaultAccess.fromEntity(vault);
	}

	@PUT
	@Path("/{vaultId}/members/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a member to this vault")
	@APIResponse(responseCode = "201", description = "member added")
	@APIResponse(responseCode = "404", description = "vault or user not found")
	public Response addMember(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		vault.members.add(user);
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
	@APIResponse(responseCode = "404", description = "vault or group not found")
	public Response addGroup(@PathParam("vaultId") String vaultId, @PathParam("groupId") String groupId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var group = Group.<Group>findByIdOptional(groupId).orElseThrow(NotFoundException::new);
		vault.groups.add(group);
		vault.persist();
		return Response.status(Response.Status.CREATED).build();
	}

	@DELETE
	@Path("/{vaultId}/members/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a member from this vault", description = "revokes the given user's access rights from this vault. If the given user is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "member removed")
	@APIResponse(responseCode = "404", description = "vault not found")
	public Response removeMember(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		vault.members.removeIf(u -> u.id.equals(userId));
		vault.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
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
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		vault.groups.removeIf(g -> g.id.equals(groupId));
		vault.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@Path("/{vaultId}/devices-requiring-access-grant")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
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
	@APIResponse(responseCode = "403", description = "device not authorized to access this vault")
	@APIResponse(responseCode = "404", description = "unknown device")
	public String unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		var currentUserId = jwt.getSubject();
		var userAccess = UserAccess.unlock(vaultId, deviceId, currentUserId);
		var groupAccess = GroupAccess.unlock(vaultId, deviceId, currentUserId);
		if (userAccess != null) {
			return userAccess.jwe;
		} else if (groupAccess != null) {
			return groupAccess.jwe;
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
	@APIResponse(responseCode = "404", description = "specified vault or device not found")
	@APIResponse(responseCode = "409", description = "Access to vault for device already granted")
	public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, String jwe) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var device = Device.<Device>findByIdOptional(deviceId).orElseThrow(NotFoundException::new);

		var userAccess = new UserAccess();
		userAccess.vault = vault;
		userAccess.user = device.owner;
		userAccess.device = device;
		userAccess.jwe = jwe;

		var groupAccesses = new ArrayList<GroupAccess>();
		for (Group userGroup : device.owner.groups) {
			if (vault.groups.contains(userGroup)) {
				var groupAccess = new GroupAccess();
				groupAccess.vault = vault;
				groupAccess.group = userGroup;
				groupAccess.device = device;
				groupAccess.jwe = jwe;
				groupAccesses.add(groupAccess);
			}
		}

		try {
			userAccess.persistAndFlush();
			for (GroupAccess groupAccess : groupAccesses) {
				// TODO handle ConstraintViolationException if access already granted because some group member may already have permission
				groupAccess.persistAndFlush();
			}
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
	public VaultDto get(@PathParam("vaultId") String vaultId) {
		// TODO: check if user has permission to access this vault?
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		return new VaultDto(vaultId, vault.name, vault.masterkey, vault.iterations, vault.salt);
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("vault-owner")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Transactional
	@Operation(summary = "creates a vault")
	@APIResponse(responseCode = "201", description = "vault created")
	@APIResponse(responseCode = "409", description = "vault with given id already exists")
	public Response create(@PathParam("vaultId") String vaultId, VaultDto vaultDto) {
		if (vaultDto == null) {
			throw new BadRequestException("Missing vault dto");
		}
		if (Vault.findByIdOptional(vaultId).isPresent()) {
			throw new ClientErrorException(Response.Status.CONFLICT);
		}
		var currentUser = User.<User>findById(jwt.getSubject());
		var vault = vaultDto.toVault(currentUser, vaultId);
		Vault.persist(vault);
		return Response.created(URI.create(".")).build();
	}

	public record VaultDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("masterkey") String masterkey, @JsonProperty("iterations") String iterations,
						   @JsonProperty("salt") String salt) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.masterkey, entity.iterations, entity.salt);
		}

		public Vault toVault(User owner, String id) {
			var vault = new Vault();
			vault.id = id;
			vault.owner = owner;
			vault.name = name;
			vault.masterkey = masterkey;
			vault.iterations = iterations;
			vault.salt = salt;
			return vault;
		}
	}

	public record VaultAccess(@JsonProperty("id") String id, @JsonProperty("users") List<UsersResource.UserDto> users, @JsonProperty("groups") List<GroupResource.GroupDto> groups) {
		public static VaultAccess fromEntity(Vault entity) {
			var users = entity.members.stream().map(UsersResource.UserDto::fromEntity).toList();
			var groups = entity.groups.stream().map(GroupResource.GroupDto::fromEntity).toList();
			return new VaultAccess(entity.id, users, groups);
		}
	}
}
