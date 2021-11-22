package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Access;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hibernate.exception.ConstraintViolationException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
	public List<VaultDto> getAll() {
		var currentUserId = jwt.getSubject();
		Stream<Vault> resultStream = Vault.findAccessibleOrOwnerByUser(currentUserId);
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/members")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	public List<UsersResource.UserDto> getMembers(@PathParam("vaultId") String vaultId) {
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		return vault.members.stream().map(UsersResource.UserDto::fromEntity).toList();
	}

	@PUT
	@Path("/{vaultId}/members/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMember(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		vault.members.add(user);
		vault.persist();
		return Response.status(Response.Status.CREATED).build();
	}

	@GET
	@Path("/{vaultId}/devices-requiring-access-grant")
	@RolesAllowed("vault-owner")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	public List<DeviceResource.DeviceDto> getDevicesRequiringAccessGrant(@PathParam("vaultId") String vaultId) {
		return Device.findRequiringAccessGrant(vaultId).map(DeviceResource.DeviceDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	public Response unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		// FIXME validate parameter

		var currentUserId = jwt.getSubject();
		var access = Access.unlock(vaultId, deviceId, currentUserId);
		Device device = Device.findById(deviceId);

		if (device == null) {
			// no such device
			return Response.status(Response.Status.NOT_FOUND).build();
		} else if (access == null) {
			// device exists, but access has not been granted
			return Response.status(Response.Status.FORBIDDEN).build();
		} else {
			var dto = new AccessGrantDto(access.deviceSpecificMasterkey, access.ephemeralPublicKey);
			return Response.ok(dto).build();
		}
	}

	@PUT
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("vault-owner")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, AccessGrantDto dto) {
		// FIXME validate parameter

		Vault vault = Vault.findById(vaultId);
		Device device = Device.findById(deviceId);

		if (vault == null || device == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		var access = new Access();
		access.vault = vault;
		access.device = device;
		access.deviceSpecificMasterkey = dto.deviceSpecificMasterkey;
		access.ephemeralPublicKey = dto.ephemeralPublicKey;

		try {
			access.persist();
			return Response.noContent().build();
		} catch (PersistenceException e) {
			if (e.getCause() instanceof ConstraintViolationException) {
				return Response.status(Response.Status.CONFLICT).build();
			} else {
				throw e; // will cause error 500
			}
		}
	}

	@DELETE
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("vault-owner")
	@Transactional
	public Response revokeDeviceAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		try {
			Access.deleteDeviceAccess(vaultId, deviceId);
			return Response.noContent().build();
		} catch (EntityNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@DELETE
	@Path("/{vaultId}/revoke-user/{userId}")
	@RolesAllowed("vault-owner")
	@Transactional
	public Response revokeUserAccess(@PathParam("vaultId") String vaultId, @PathParam("userId") String userId) {
		try {
			Access.deleteUserAccess(vaultId, userId);
			return Response.noContent().build();
		} catch (EntityNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response get(@PathParam("vaultId") String vaultId) {
		Vault vault = Vault.findById(vaultId);
		if (vault == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		var dto = new VaultDto(vaultId, vault.name, vault.masterkey, vault.iterations, vault.salt);
		return Response.ok(dto).build();
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("vault-owner")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Transactional
	public Response create(/*@Valid*/ VaultDto vaultDto, @PathParam("vaultId") String vaultId) {
		// FIXME verify uuid

		if (vaultDto == null) {
			return Response.serverError().entity("Vault cannot be null").build();
		}

		if (Vault.findByIdOptional(vaultId).isPresent()) {
			return Response.status(Response.Status.CONFLICT).build();
		}

		User currentUser = User.findById(jwt.getSubject());
		var vault = vaultDto.toVault(currentUser, vaultId);
		//TODO: can the persisted id different?
		Vault.persist(vault);
		return Response.ok(vault.id).build();
	}

	public static record AccessGrantDto(@JsonProperty("device_specific_masterkey") String deviceSpecificMasterkey, @JsonProperty("ephemeral_public_key") String ephemeralPublicKey) {
	}

	public static record VaultDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("masterkey") String masterkey, @JsonProperty("iterations") String iterations, @JsonProperty("salt") String salt) {

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

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.masterkey, entity.iterations, entity.salt);
		}
	}
}
