package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.filters.ActiveLicense;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Transactional
	@Operation(summary = "sync the logged-in user from the remote user provider to hub")
	@APIResponse(responseCode = "201", description = "user created or updated")
	public Response syncMe() {
		var userId = jwt.getSubject();
		User user = User.findById(userId);
		if (user == null) {
			user = new User();
			user.id = userId;
		}
		user.name = jwt.getName();
		user.pictureUrl = jwt.getClaim("picture");
		user.email = jwt.getClaim("email");
		user.persist();
		return Response.created(URI.create(".")).build();
	}

	@PUT
	@Path("/me/key-pair")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "update the logged-in user, storing provided key pair and key derivation parameters")
	@APIResponse(responseCode = "201", description = "user updated")
	public Response putKey(@Valid UserDto dto) {
		var userId = jwt.getSubject();
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		user.publicKey = dto.publicKey;
		user.privateKey = dto.privateKey;
		user.salt = dto.salt;
		user.iterations = dto.iterations;
		user.persist();
		return Response.created(URI.create(".")).build();
	}

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices, @QueryParam("withAccessibleVaults") boolean withAccessibleVaults) {
		User user = User.findById(jwt.getSubject());
		Function<AccessToken, VaultResource.VaultDto> mapAccessibleVaults = a -> new VaultResource.VaultDto(a.vault.id, a.vault.name, a.vault.description, a.vault.creationTime.truncatedTo(ChronoUnit.MILLIS), null, 0, null, null, null);
		Function<Device, DeviceResource.DeviceDto> mapDevices = d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.userKeyJwe, d.owner.id, d.creationTime.truncatedTo(ChronoUnit.MILLIS));
		var devices = withDevices ? user.devices.stream().map(mapDevices).collect(Collectors.toSet()) : Set.<DeviceResource.DeviceDto>of();
		var vaults = withAccessibleVaults ? user.accessTokens.stream().map(mapAccessibleVaults).collect(Collectors.toSet()) : Set.<VaultResource.VaultDto>of();
		return new UserDto(user.id, user.name, user.pictureUrl, user.email, devices, vaults, user.publicKey, user.privateKey, user.salt, user.iterations);
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		return User.findAll().<User>stream().map(UserDto::justPublicInfo).toList();
	}

	@GET
	@Path("/me/device-tokens/{deviceId}")
	@RolesAllowed("user")
	@Produces(MediaType.TEXT_PLAIN)
	@NoCache
	@Transactional
	@Operation(summary = "get the device-specific user key", description = "retrieves the user jwe for the specified device")
	@APIResponse(responseCode = "200", description = "Device found")
	@APIResponse(responseCode = "403", description = "Device not yet verified")
	@APIResponse(responseCode = "404", description = "Device not found or owned by a different user")
	@ActiveLicense
	public String getMe(@PathParam("deviceId") @ValidId String deviceId) {
		try {
			var device = Device.findByIdAndUser(deviceId, jwt.getSubject());
			if (device.userKeyJwe == null) {
				throw new ForbiddenException("Device needs verification");
			} else {
				return device.userKeyJwe;
			}
		} catch (NoResultException e) {
			throw new NotFoundException(e);
		}
	}

}