package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.RemoteUserProviderFactory;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	SyncerConfig syncerConfig;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "201", description = "user created")
	public Response syncMe() {
		// TODO delete user sync to update access credentials
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
				a -> new VaultResource.VaultDto(a.vault.id, a.vault.name, a.vault.description, a.vault.creationTime, UserDto.fromEntity(a.vault.owner), null, null, null);
		Function<Device, DeviceResource.DeviceDto> mapDevices = withAccessibleVaults //
				? d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id, d.accessTokens.stream().map(mapAccessibleVaults).collect(Collectors.toSet())) //
				: d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id, Set.of());
		return withDevices //
				? new UserDto(user.id, user.name, user.pictureUrl, user.email, user.devices.stream().map(mapDevices).collect(Collectors.toSet()))
				: new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of());
	}

	@GET
	@Path("/")
	@RolesAllowed("vault-owner")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		return User.findAll().<User>stream().map(UserDto::fromEntity).toList();
	}

	@GET
	@Path("/search")
	@RolesAllowed("vault-owner")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "search user")
	public List<UserDto> search(@QueryParam("querry") String querry) {
		return new RemoteUserProviderFactory().get(syncerConfig).searchUser(querry).map(UserDto::fromEntity).toList();
	}

	public static final class UserDto extends AuthorityDto {
		@JsonProperty("pictureUrl")
		public final String pictureUrl;
		@JsonProperty("email")
		public final String email;
		@JsonProperty("devices")
		public final Set<DeviceResource.DeviceDto> devices;

		UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {
			super(id, "user", name); // TODO keep string "user"?
			this.pictureUrl = pictureUrl;
			this.email = email;
			this.devices = devices;
		}

		public static UserDto fromEntity(User user) {
			return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of());
		}
	}
}