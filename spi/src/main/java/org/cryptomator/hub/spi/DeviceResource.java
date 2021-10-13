package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.Device;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/devices")
public class DeviceResource {

	@Inject
	UserInfo userInfo;

	@Inject
	UserDao userDao;

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
		if (Device.findByIdOptional(deviceId).isEmpty() ) {
			var currentUser = userDao.get(userInfo.getString("sub"));
			var device = deviceDto.toDevice(currentUser, deviceId);
			device.persist(device);
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
		Device device = Device.findById(deviceId);
		if (device != null) {
			return Response.ok(new DeviceDto(device.id, device.name, device.publickey, null)).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		List<Device> devices = Device.listAll();
		var dtos = devices.stream().map(d -> new DeviceDto(d.id, d.name, d.publickey, null)).collect(Collectors.toList());
		return Response.ok(dtos).build();
	}

	public static class DeviceDto {

		private final String id;
		private final String name;
		private final String publicKey;
		private final Set<VaultResource.VaultDto> accessTo;

		public DeviceDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("publicKey") String publicKey, @JsonProperty("accessTo") Set<VaultResource.VaultDto> accessTo) {
			this.id = id;
			this.name = name;
			this.publicKey = publicKey;
			this.accessTo = accessTo;
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
			device.id = id;
			device.owner = user;
			device.name = getName();
			device.publickey = getPublicKey();
			return device;
		}

		public Set<VaultResource.VaultDto> getAccessTo() {
			return accessTo;
		}
	}
}
