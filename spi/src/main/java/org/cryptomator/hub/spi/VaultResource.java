package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.Access;
import org.cryptomator.hub.persistence.entities.AccessDao;
import org.cryptomator.hub.persistence.entities.DeviceDao;
import org.cryptomator.hub.persistence.entities.User;
import org.cryptomator.hub.persistence.entities.UserDao;
import org.cryptomator.hub.persistence.entities.Vault;
import org.cryptomator.hub.persistence.entities.VaultDao;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
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
    @Produces(MediaType.TEXT_PLAIN)
    public Response unlock(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId) {
        // FIXME validate parameter

        var deviceAccess = accessDao.get(vaultId, deviceId);
        var currentUserId = userInfo.getString("sub");

        if (deviceAccess == null || !deviceAccess.getDevice().getUser().getId().equals(currentUserId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(deviceAccess.getDeviceSpecificMasterkey()).build();
    }

    @PUT
    @Path("/{vaultId}/keys/{deviceId}")
    @RolesAllowed("user")
    @Transactional
    @Consumes(MediaType.TEXT_PLAIN)
    public Response grantAccess(@PathParam("vaultId") String vaultId, @PathParam("deviceId") String deviceId, String deviceSpecificMasterkey) {
        var access = new Access();
        access.setVault(vaultDao.get(vaultId));
        access.setDevice(deviceDao.get(deviceId));
        access.setDeviceSpecificMasterkey(deviceSpecificMasterkey);
        accessDao.persist(access);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{uuid}")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response create(/*@Valid*/ VaultDto vaultDto, @PathParam("uuid") String uuid) {
        // FIXME verify uuid

        if (vaultDto == null) {
            return Response.serverError().entity("Vault cannot be null").build();
        }

        if (vaultDao.get(uuid) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        var currentUser = userDao.get(userInfo.getString("sub"));
        var vault = vaultDto.toVault(currentUser, uuid);
        var persistedVaultId = vaultDao.persist(vault);

        return Response.ok(persistedVaultId).build();
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

        public Vault toVault(User user, String uuid) {
            var vault = new Vault();
            vault.setId(uuid);
            vault.setName(getName());
            vault.setMasterkey(getMasterkey());
            vault.setIterations(getIterations());
            vault.setSalt(getSalt());
            vault.setUser(user);
            return vault;
        }
    }
}
