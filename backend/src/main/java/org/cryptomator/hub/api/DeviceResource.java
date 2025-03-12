package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.LegacyAccessToken;
import org.cryptomator.hub.entities.LegacyDevice;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultKeyRetrievedEvent;
import org.cryptomator.hub.validation.NoHtmlOrScriptChars;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidId;
import org.cryptomator.hub.validation.ValidJWE;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/devices")
public class DeviceResource {

	private static final Logger LOG = Logger.getLogger(DeviceResource.class);

	@Inject
	EventLogger eventLogger;
	@Inject
	User.Repository userRepo;
	@Inject
	Device.Repository deviceRepo;
	@Inject
	LegacyAccessToken.Repository legacyAccessTokenRepo;
	@Inject
	LegacyDevice.Repository legacyDeviceRepo;

	@Inject
	JsonWebToken jwt;

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "lists all devices matching the given ids", description = "lists for each id in the list its corresponding device. Ignores all id's where a device cannot be found")
	@APIResponse(responseCode = "200")
	public List<DeviceDto> getSome(@QueryParam("ids") List<String> deviceIds) {
		return deviceRepo.findAllInList(deviceIds).map(DeviceDto::fromEntity).toList();
	}

	@Deprecated
	@GET
	@Path("/legacy-devices")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "lists all legacy devices matching the given ids", description = "lists for each id in the list its corresponding legacy device. Ignores all id's where a device cannot be found")
	@APIResponse(responseCode = "200")
	public List<DeviceDto> getSomeLegacy(@QueryParam("ids") List<String> deviceIds) {
		return legacyDeviceRepo.findAllInList(deviceIds).map(DeviceDto::fromEntity).toList();
	}

	@PUT
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates or updates a device", description = "the device will be owned by the currently logged-in user")
	@APIResponse(responseCode = "201", description = "Device created or updated")
	@APIResponse(responseCode = "409", description = "Device with this key already exists")
	public Response createOrUpdate(@Valid @NotNull DeviceDto dto, @PathParam("deviceId") @ValidId String deviceId) {
		Device device;
		try {
			device = deviceRepo.findByIdAndUser(deviceId, jwt.getSubject());
		} catch (NoResultException e) {
			device = new Device();
			device.setId(deviceId);
			device.setOwner(userRepo.findById(jwt.getSubject()));
			device.setCreationTime(Instant.now().truncatedTo(ChronoUnit.MILLIS));
			device.setType(dto.type != null ? dto.type : Device.Type.DESKTOP); // default to desktop for backwards compatibilit);

			if (legacyDeviceRepo.deleteById(device.getId())) {
				assert legacyDeviceRepo.findById(device.getId()) == null;
				LOG.info("Deleted Legacy Device during re-registration of Device " + deviceId);
			}
		}
		device.setName(dto.name);
		device.setPublickey(dto.publicKey);
		device.setUserPrivateKeys(dto.userPrivateKeys);

		try {
			deviceRepo.persistAndFlush(device);
			eventLogger.logDeviceRegisted(jwt.getSubject(), deviceId, device.getName(), device.getType());
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			throw new ClientErrorException(Response.Status.CONFLICT, e);
		}
	}

	@GET
	@Path("/{deviceId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the device", description = "the device must be owned by the currently logged-in user")
	@APIResponse(responseCode = "200", description = "Device found")
	@APIResponse(responseCode = "404", description = "Device not found or owned by a different user")
	public DeviceDto get(@PathParam("deviceId") @ValidId String deviceId) {
		try {
			Device device = deviceRepo.findByIdAndUser(deviceId, jwt.getSubject());
			return DeviceDto.fromEntity(device);
		} catch (NoResultException e) {
			throw new NotFoundException(e);
		}
	}

	@Deprecated
	@GET
	@Path("/{deviceId}/legacy-access-tokens")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "list legacy access tokens", description = "get all legacy access tokens for this device ({vault1: token1, vault1: token2, ...}). The device must be owned by the currently logged-in user")
	@APIResponse(responseCode = "200")
	public Map<UUID, String> getLegacyAccessTokens(@PathParam("deviceId") @ValidId String deviceId) {
		return legacyAccessTokenRepo.getByDeviceAndOwner(deviceId, jwt.getSubject())
				.collect(Collectors.toMap(token -> token.getId().getVaultId(), LegacyAccessToken::getJwe));
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
		return remove(deviceId, false);
	}

	@Deprecated
	@DELETE
	@Path("/{deviceId}/legacy-device")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "removes a legacy device", description = "the legacy device will be only be removed if the current user is the owner")
	@APIResponse(responseCode = "204", description = "legacy device removed")
	@APIResponse(responseCode = "404", description = "legacy device not found with current user")
	public Response removeLegacyDevice(@PathParam("deviceId") @ValidId String deviceId) {
		return remove(deviceId, true);
	}

	private Response remove(String deviceId, boolean legacyDevice) {
		if (deviceId == null || deviceId.trim().isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST).entity("deviceId cannot be empty").build();
		}
		User currentUser = userRepo.findById(jwt.getSubject());
		boolean removed = legacyDevice
				? remove(deviceId, currentUser, legacyDeviceRepo::findByIdOptional, legacyDeviceRepo::delete, LegacyDevice::getOwner)
				: remove(deviceId, currentUser, deviceRepo::findByIdOptional, deviceRepo::delete, Device::getOwner);
		if (removed) {
			eventLogger.logDeviceRemoved(jwt.getSubject(), deviceId);
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	private <T> boolean remove(String deviceId, User currentUser, Function<String, Optional<T>> findById, Consumer<T> delete, Function<T, User> getOwner) {
		Optional<T> maybeDevice = findById.apply(deviceId);
		if (maybeDevice.isPresent() && currentUser.equals(getOwner.apply(maybeDevice.get()))) {
			delete.accept(maybeDevice.get());
			return true;
		}
		return false;
	}

	public record DeviceDto(@JsonProperty("id") @ValidId String id,
							@JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
							@JsonProperty("type") Device.Type type,
							@JsonProperty("publicKey") @NotNull @OnlyBase64Chars String publicKey,
							@JsonProperty("userPrivateKey") @NotNull @ValidJWE String userPrivateKeys, // singular name for history reasons (don't break client compatibility)
							@JsonProperty("owner") @ValidId String ownerId,
							@JsonProperty("creationTime") Instant creationTime,
							@JsonProperty("lastIpAddress") String lastIpAddress,
							@JsonProperty("lastAccessTime") Instant lastAccessTime,
							@JsonProperty("legacyDevice") boolean legacyDevice) {

		public static DeviceDto fromEntity(Device entity) {
			return new DeviceDto(entity.getId(), entity.getName(), entity.getType(), entity.getPublickey(), entity.getUserPrivateKeys(), entity.getOwner().getId(), entity.getCreationTime().truncatedTo(ChronoUnit.MILLIS), null, null, false);
		}

		@Deprecated
		public static DeviceDto fromEntity(LegacyDevice entity) {
			return new DeviceDto(entity.getId(), entity.getName(), entity.getType(), entity.getPublickey(), null, entity.getOwner().getId(), entity.getCreationTime().truncatedTo(ChronoUnit.MILLIS), null, null, true);
		}

		public static DeviceDto fromEntity(Device d, @Nullable VaultKeyRetrievedEvent event) {
			var lastIpAddress = (event != null) ? event.getIpAddress() : null;
			var lastAccessTime = (event != null) ? event.getTimestamp() : null;
			return new DeviceResource.DeviceDto(d.getId(), d.getName(), d.getType(), d.getPublickey(), d.getUserPrivateKeys(), d.getOwner().getId(), d.getCreationTime().truncatedTo(ChronoUnit.MILLIS), lastIpAddress, lastAccessTime, false);
		}

		@Deprecated
		public static DeviceDto fromEntity(LegacyDevice d, @Nullable VaultKeyRetrievedEvent event) {
			var lastIpAddress = (event != null) ? event.getIpAddress() : null;
			var lastAccessTime = (event != null) ? event.getTimestamp() : null;
			return new DeviceResource.DeviceDto(d.getId(), d.getName(), d.getType(), d.getPublickey(), null, d.getOwner().getId(), d.getCreationTime().truncatedTo(ChronoUnit.MILLIS), lastIpAddress, lastAccessTime, true);
		}

	}

	public record LegacyAccessTokenDto(@JsonProperty("vaultId") UUID vaultId, @JsonProperty("token") String token) {
	}
}
