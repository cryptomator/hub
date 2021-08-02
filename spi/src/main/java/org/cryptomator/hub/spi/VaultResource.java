package org.cryptomator.hub.spi;

import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.Access;
import org.cryptomator.hub.persistence.entities.AccessDao;
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
import java.util.stream.Collectors;

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

    @GET
    @Path("/{id}/keys/{deviceId}")
    @RolesAllowed("user")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response unlock(@PathParam("id") String id, @PathParam("deviceId") String deviceId) {
        // FIXME validate parameter

        var deviceAccess = accessDao.get(id, deviceId);
        var currentUserId = userInfo.getString("sub");

        if (deviceAccess == null || !deviceAccess.getDevice().getUser().getId().equals(currentUserId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(deviceAccess.getDeviceSpecificMasterkey()).build();
    }

    @PUT
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response create(/*@Valid*/ VaultDto vaultDto) {
        if (vaultDto == null) {
            return Response.serverError().entity("Vault cannot be null").build();
        }

        if (vaultDao.get(vaultDto.uuid) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        var currentUser = userDao.get(userInfo.getString("sub"));

        var vault = new Vault();
        vault.setId(vaultDto.uuid);
        vault.setName(vaultDto.name);
        vault.setMasterkey(vaultDto.masterKey);
        vault.setCostParam("default"); // TODO
        vault.setSalt("default"); // TODO
        vault.setUser(currentUser);

        var persistedVaultId = vaultDao.persist(vault);
        createAccessEntriesFor(vault);

        return Response.ok(persistedVaultId).build();
    }

    private void createAccessEntriesFor(Vault vault) {
        var user = vault.getUser();
        var result = user.getDevices().stream().map(device -> {
                    var access = new Access();
                    access.setId(new Access.AccessId(device.getId(), vault.getId()));
                    access.setVault(vault);
                    access.setDevice(device);
                    access.setDeviceSpecificMasterkey(""); // TODO encrypt masterkey with devices public key
                    return access;
                }).map(access -> accessDao.persist(access))
                .collect(Collectors.toList());
    }

    public static class VaultDto {

        private final String uuid;
        private final String name;
        private final String masterKey;

        public VaultDto(String uuid, String name, String masterKey) {
            this.uuid = uuid;
            this.name = name;
            this.masterKey = masterKey;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
