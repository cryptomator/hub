package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.cryptomator.hub.entities.Access;
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

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "201", description = "user created")
	public Response syncMe() {
		User.createOrUpdate(jwt.getSubject(), jwt.getName(), jwt.getClaim("picture"), jwt.getClaim("email"));
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
		Function<Access, VaultResource.VaultDto> mapAccessibleVaults = a -> new VaultResource.VaultDto(a.vault.id, a.vault.name, null, null, null);
		Function<Device, DeviceResource.DeviceDto> mapDevices = withAccessibleVaults //
				? d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id, d.access.stream().map(mapAccessibleVaults).collect(Collectors.toSet())) //
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
		PanacheQuery<User> query = User.findAll();
		return query.stream().map(UserDto::fromEntity).toList();
	}

	@SuppressWarnings("ClassCanBeRecord") // wait for https://github.com/quarkusio/quarkus/issues/20891
	public static class UserDto {

		private final String id;
		private final String name;
		private final String pictureUrl;
		private final String email;
		private final Set<DeviceResource.DeviceDto> devices;

		@JsonCreator
		public UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {
			this.id = id;
			this.name = name;
			this.pictureUrl = pictureUrl;
			this.email = email;
			this.devices = devices;
		}

		public static UserDto fromEntity(User user) {
			return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of());
		}

		@JsonProperty("id")
		public String getId() {
			return id;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("pictureUrl")
		public String getPictureUrl() {
			return pictureUrl;
		}

		@JsonProperty("email")
		public String getEmail() {
			return email;
		}

		@JsonProperty("devices")
		public Set<DeviceResource.DeviceDto> getDevices() {
			return devices;
		}
	}

}