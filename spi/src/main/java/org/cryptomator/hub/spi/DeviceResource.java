package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.Device;
import org.cryptomator.hub.persistence.entities.DeviceDao;
import org.cryptomator.hub.persistence.entities.User;
import org.cryptomator.hub.persistence.entities.UserDao;

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

@Path("/devices")
public class DeviceResource {

    @Inject
    UserInfo userInfo;

    @Inject
    UserDao userDao;

    @Inject
    DeviceDao deviceDao;

    @PUT
    @Path("/{uuid}")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(/*@Valid*/ DeviceDto deviceDto, @PathParam("uuid") String uuid) {
        // FIXME validate parameter
        if (uuid == null || uuid.trim().length() == 0 || deviceDto == null) {
            return Response.serverError().entity("UUID cannot be empty").build();
        }
        if (deviceDao.get(uuid) == null) {
            var currentUser = userDao.get(userInfo.getString("sub"));
            var device = deviceDto.toDevice(currentUser, uuid);
            var storedDeviceId = deviceDao.persist(device);
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.CONFLICT).build();
        }
    }

    @GET
    @Path("/")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        var devices = deviceDao.getAll();
        var dtos = devices.stream().map(d -> new DeviceDto(d.getName(), d.getPublickey())).collect(Collectors.toList());
        return Response.status(Response.Status.OK).entity(dtos).build();
    }

    public static class DeviceDto {

        private final String name;
        private final String publicKey;

        public DeviceDto(@JsonProperty("name") String name, @JsonProperty("publicKey") String publicKey) {
            this.name = name;
            this.publicKey = publicKey;
        }

        public String getName() {
            return name;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public Device toDevice(User user, String uuid) {
            var device = new Device();
            device.setId(uuid);
            device.setUser(user);
            device.setName(getName());
            device.setPublickey(getPublicKey());
            return device;
        }
    }
}
