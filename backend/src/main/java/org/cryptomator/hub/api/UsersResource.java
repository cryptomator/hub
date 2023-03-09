package org.cryptomator.hub.api;

import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices, @QueryParam("withAccessibleVaults") boolean withAccessibleVaults) {
		User user = User.findById(jwt.getSubject());
		Function<AccessToken, VaultResource.VaultDto> mapAccessibleVaults =
				a -> new VaultResource.VaultDto(a.vault.id, a.vault.name, a.vault.description, a.vault.creationTime.truncatedTo(ChronoUnit.MILLIS), null, null, null, null, null);
		Function<Device, DeviceResource.DeviceDto> mapDevices = withAccessibleVaults //
				? d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id, d.accessTokens.stream().map(mapAccessibleVaults).collect(Collectors.toSet()), d.creationTime.truncatedTo(ChronoUnit.MILLIS)) //
				: d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id, Set.of(), d.creationTime.truncatedTo(ChronoUnit.MILLIS));
		return withDevices //
				? new UserDto(user.id, user.name, user.pictureUrl, user.email, user.devices.stream().map(mapDevices).collect(Collectors.toSet()))
				: new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of());
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		return User.findAll().<User>stream().map(UserDto::fromEntity).toList();
	}

}