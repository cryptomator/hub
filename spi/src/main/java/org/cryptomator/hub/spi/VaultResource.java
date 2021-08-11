package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.Access;
import org.cryptomator.hub.persistence.entities.AccessDao;
import org.cryptomator.hub.persistence.entities.DeviceDao;
import org.cryptomator.hub.persistence.entities.User;
import org.cryptomator.hub.persistence.entities.UserDao;
import org.cryptomator.hub.persistence.entities.Vault;
import org.cryptomator.hub.persistence.entities.VaultDao;
import org.hibernate.exception.ConstraintViolationException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/vaults")
public class VaultResource {

	@Inject
	UserInfo userInfo;

	@Inject
	AccessDao accessDao;

	@Inject
	UserDao userDao;

	@Inject
	VaultDao vaultDao;

	@Inject
	DeviceDao deviceDao;

	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	public Response unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		// FIXME validate parameter

		var deviceAccess = accessDao.get(vaultId, deviceId);
		var currentUserId = userInfo.getString("sub");

		if (deviceAccess == null || !deviceAccess.getDevice().getUser().getId().equals(currentUserId)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		var dto = new AccessGrantDto(deviceAccess.getDeviceSpecificMasterkey(), deviceAccess.getEphemeralPublicKey());

		return Response.ok(dto).build();
	}

	@PUT
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, AccessGrantDto dto) {
		// FIXME validate parameter

		var vault = vaultDao.get(vaultId);
		var device = deviceDao.get(deviceId);

		if (vault == null || device == null ) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		var access = new Access();
		access.setVault(vault);
		access.setDevice(device);
		access.setDeviceSpecificMasterkey(dto.deviceSpecificMasterkey);
		access.setEphemeralPublicKey(dto.ephemeralPublicKey);

		try {
			accessDao.persist(access);
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
	@RolesAllowed("user")
	@Transactional
	public Response revokeAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
		try {
			accessDao.delete(vaultId, deviceId);
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
		var vault = vaultDao.get(vaultId);
		if (vault == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		var dto = new VaultDto(vault.getName(), vault.getMasterkey(), vault.getIterations(), vault.getSalt());
		return Response.ok(dto).build();
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Transactional
	public Response create(/*@Valid*/ VaultDto vaultDto, @PathParam("vaultId") String vaultId) {
		// FIXME verify uuid

		if (vaultDto == null) {
			return Response.serverError().entity("Vault cannot be null").build();
		}

		if (vaultDao.get(vaultId) != null) {
			return Response.status(Response.Status.CONFLICT).build();
		}

		var currentUser = userDao.get(userInfo.getString("sub"));
		var vault = vaultDto.toVault(currentUser, vaultId);
		var persistedVaultId = vaultDao.persist(vault);

		return Response.ok(persistedVaultId).build();
	}

	public static class AccessGrantDto {
		@JsonProperty("device_specific_masterkey")
		public final String deviceSpecificMasterkey;

		@JsonProperty("ephemeral_public_key")
		public final String ephemeralPublicKey;

		@JsonCreator
		public AccessGrantDto(@JsonProperty("device_specific_masterkey") String deviceSpecificMasterkey, @JsonProperty("ephemeral_public_key") String ephemeralPublicKey) {
			this.deviceSpecificMasterkey = deviceSpecificMasterkey;
			this.ephemeralPublicKey = ephemeralPublicKey;
		}
	}

	public static class VaultDto {

		private final String name;
		private final String masterkey;
		private final String iterations;
		private final String salt;

		public VaultDto(@JsonProperty("name") String name, @JsonProperty("masterkey") String masterkey, @JsonProperty("iterations") String iterations, @JsonProperty("salt") String salt) {
			this.name = name;
			this.masterkey = masterkey;
			this.iterations = iterations;
			this.salt = salt;
		}

		public String getName() {
			return name;
		}

		public String getMasterkey() {
			return masterkey;
		}

		public String getIterations() {
			return iterations;
		}

		public String getSalt() {
			return salt;
		}

		public Vault toVault(User user, String id) {
			var vault = new Vault();
			vault.setId(id);
			vault.setName(getName());
			vault.setMasterkey(getMasterkey());
			vault.setIterations(getIterations());
			vault.setSalt(getSalt());
			vault.setUser(user);
			return vault;
		}
	}
}
