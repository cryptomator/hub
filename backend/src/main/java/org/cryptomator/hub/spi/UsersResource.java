package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@NoCache
	public Response syncMe() {
		User.createOrUpdate(jwt.getSubject(), jwt.getName(), jwt.getClaim("picture"));
		return Response.created(URI.create(".")).build();
	}

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@NoCache
	public String getMe() {
		User user = User.findById(jwt.getSubject());
		return user.name;
	}

	@GET
	@Path("/me-extended")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeIncludingDevicesAndVaults() {
		User user = User.getWithDevicesAndAccess(jwt.getSubject()); // TODO: fix NPE if user not found
		var devices = user
				.devices
				.stream()
				.map(device -> new DeviceResource.DeviceDto(device.id, device.name, device.publickey, device.owner.id, device.access.stream().map(access -> {
					var vault = access.vault;
					return new VaultResource.VaultDto(vault.id, vault.name, null, null, null);
				}).collect(Collectors.toSet())))
				.collect(Collectors.toSet());
		return Response.ok(new UserDto(user.id, user.name, user.pictureUrl, devices)).build();
	}

	@GET
	@Path("/devices")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllIncludingDevices() {
		var users = User.getAllWithDevicesAndAccess().stream().map(user -> {
			var devices = user
					.devices
					.stream()
					.map(device -> new DeviceResource.DeviceDto(device.id, device.name, device.publickey, device.owner.id, device
							.access
							.stream()
							.map(access -> new VaultResource.VaultDto(access.id.getVaultId(), null, null, null, null))
							.collect(Collectors.toSet())))
					.collect(Collectors.toSet());
			return new UserDto(user.id, user.name, user.pictureUrl, devices);
		}).collect(Collectors.toList());
		return Response.ok(users).build();
	}

    public static record UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl,
                                 @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {

		public static UserDto fromEntity(User user) {
			return new UserDto(user.id, user.name, user.pictureUrl, Set.of());
		}
	}

}