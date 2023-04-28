package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.NoHtmlOrScriptChars;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.OnlyBase64UrlChars;
import org.cryptomator.hub.validation.ValidId;
import org.cryptomator.hub.validation.ValidJWE;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
	public Response create(@Valid DeviceDto deviceDto, @PathParam("deviceId") @ValidId String deviceId) {
		if (deviceId == null || deviceId.trim().length() == 0 || deviceDto == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("deviceId or deviceDto cannot be empty").build();
		}
		User currentUser = User.findById(jwt.getSubject());
		var device = deviceDto.toDevice(currentUser, deviceId, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		try {
			device.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (PersistenceException e) {
			if (e instanceof ConstraintViolationException) {
				throw new ClientErrorException(Response.Status.CONFLICT, e);
			} else {
				throw new InternalServerErrorException(e);
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
	public Response remove(@PathParam("deviceId") @ValidId String deviceId) {
		if (deviceId == null || deviceId.trim().length() == 0) {
			return Response.status(Response.Status.BAD_REQUEST).entity("deviceId cannot be empty").build();
		}

		User currentUser = User.findById(jwt.getSubject());
		var maybeDevice = Device.<Device>findByIdOptional(deviceId);
		if (maybeDevice.isPresent() && currentUser.equals(maybeDevice.get().owner)) {
			maybeDevice.get().delete();
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	public record DeviceDto(@JsonProperty("id") @ValidId String id,
							@JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
							@JsonProperty("publicKey") @OnlyBase64Chars String publicKey,
							@JsonProperty("userKeyJwe") @ValidJWE String userKeyJwe,
							@JsonProperty("owner") @ValidId String ownerId,
							@JsonProperty("creationTime") Instant creationTime) {

		public Device toDevice(User user, String id, Instant creationTime) {
			var device = new Device();
			device.id = id;
			device.owner = user;
			device.name = name;
			device.publickey = publicKey;
			device.userKeyJwe = userKeyJwe;
			device.creationTime = creationTime;
			return device;
		}

		public static DeviceDto fromEntity(Device entity) {
			return new DeviceDto(entity.id, entity.name, entity.publickey, entity.userKeyJwe, entity.owner.id, entity.creationTime.truncatedTo(ChronoUnit.MILLIS));
		}

	}
}
