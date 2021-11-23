package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/devices")
public class DeviceResource {

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "adds a device", description = "the device will be owned by the currently logged-in user")
	@APIResponse(responseCode = "201", description = "device created")
	public Response create(DeviceDto deviceDto, @PathParam("deviceId") String deviceId) {
		// FIXME validate parameter
		if (deviceId == null || deviceId.trim().length() == 0 || deviceDto == null) {
			return Response.serverError().entity("deviceId or deviceDto cannot be empty").build();
		}
		if (Device.findByIdOptional(deviceId).isEmpty()) {
			User currentUser = User.findById(jwt.getSubject());
			var device = deviceDto.toDevice(currentUser, deviceId);
			Device.persist(device);
			return Response.status(Response.Status.CREATED).build();
		} else {
			return Response.status(Response.Status.CONFLICT).build();
		}
	}

	public static record DeviceDto(@JsonProperty("id") String id, @JsonProperty("name") String name,
								   @JsonProperty("publicKey") String publicKey,
								   @JsonProperty("owner") String ownerId,
								   @JsonProperty("accessTo") Set<VaultResource.VaultDto> accessTo) {

		public Device toDevice(User user, String id) {
			var device = new Device();
			device.id = id;
			device.owner = user;
			device.name = name;
			device.publickey = publicKey;
			return device;
		}

		public static DeviceDto fromEntity(Device entity) {
			return new DeviceDto(entity.id, entity.name, entity.publickey, entity.owner.id, Set.of());
		}

	}
}
