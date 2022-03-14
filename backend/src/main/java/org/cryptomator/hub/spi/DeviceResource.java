package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
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
	@APIResponse(responseCode = "409", description = "Device already exists")
	public Response create(DeviceDto deviceDto, @PathParam("deviceId") String deviceId) {
		// FIXME validate parameter
		if (deviceId == null || deviceId.trim().length() == 0 || deviceDto == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("deviceId or deviceDto cannot be empty").build();
		}
		User currentUser = User.findById(jwt.getSubject());
		var device = deviceDto.toDevice(currentUser, deviceId);
		try {
			Device.persist(device);
			Device.flush(); // flush to trigger constraint violations
			return Response.created(URI.create(".")).build();
		} catch (PersistenceException e) {
			if (e.getCause() instanceof ConstraintViolationException c) {
				return Response.status(Response.Status.CONFLICT).entity(new ConstraintViolationDto(c.getConstraintName())).build();
			} else {
				throw e;
			}
		}
	}

	@DELETE
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "removes a device", description = "the device will be only be removed if the current user is the owner")
	@APIResponse(responseCode = "204", description = "device removed")
	@APIResponse(responseCode = "404", description = "device not found with current user")
	public Response remove(@PathParam("deviceId") String deviceId) {
		// FIXME validate parameter
		if (deviceId == null || deviceId.trim().length() == 0) {
			return Response.status(Response.Status.BAD_REQUEST).entity("deviceId cannot be empty").build();
		}

		User currentUser = User.findById(jwt.getSubject());
		Optional<Device> maybeDevice = Device.findByIdOptional(deviceId);
		if (maybeDevice.isPresent() && currentUser.equals(maybeDevice.get().owner)) {
			maybeDevice.get().delete();
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
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
