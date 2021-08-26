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
import java.util.Set;
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
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response create(/*@Valid*/ DeviceDto deviceDto, @PathParam("deviceId") String deviceId) {
		// FIXME validate parameter
		if (deviceId == null || deviceId.trim().length() == 0 || deviceDto == null) {
			return Response.serverError().entity("deviceId or deviceDto cannot be empty").build();
		}
		if (deviceDao.get(deviceId) == null) {
			var currentUser = userDao.get(userInfo.getString("sub"));
			var device = deviceDto.toDevice(currentUser, deviceId);
			var storedDeviceId = deviceDao.persist(device);
			return Response.status(Response.Status.CREATED).build();
		} else {
			return Response.status(Response.Status.CONFLICT).build();
		}
	}

	@GET
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@PathParam("deviceId") String deviceId) {
		// FIXME validate parameter
		if (deviceId == null || deviceId.trim().length() == 0) {
			return Response.serverError().entity("deviceId cannot be empty").build();
		}
		var device = deviceDao.get(deviceId);
		if (device != null) {
			return Response.ok(new DeviceDto(device.getId(), device.getName(), device.getPublickey(), null)).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		var devices = deviceDao.getAll();
		var dtos = devices.stream().map(d -> new DeviceDto(d.getId(), d.getName(), d.getPublickey(), null)).collect(Collectors.toList());
		return Response.ok(dtos).build();
	}

	public static class DeviceDto {

		private final String id;
		private final String name;
		private final String publicKey;
		private final Set<String> vaultsAccessTo;

		public DeviceDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("publicKey") String publicKey, @JsonProperty("vaultsAccessTo") Set<String> vaultsAccessTo) {
			this.id = id;
			this.name = name;
			this.publicKey = publicKey;
			this.vaultsAccessTo = vaultsAccessTo;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getPublicKey() {
			return publicKey;
		}

		public Device toDevice(User user, String id) {
			var device = new Device();
			device.setId(id);
			device.setUser(user);
			device.setName(getName());
			device.setPublickey(getPublicKey());
			return device;
		}

		public Set<String> getVaultsAccessTo() {
			return vaultsAccessTo;
		}
	}
}
