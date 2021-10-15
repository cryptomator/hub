package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.entities.User;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	UserInfo userInfo;

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@NoCache
	public String me() {
		return User.findById(userInfo.getString("sub")).name;
	}

	@GET
	@Path("/me-extended")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeIncludingDevicesAndVaults() {
		User user = User.getWithDevicesAndAccess(userInfo.getString("sub"));
		var devices = user
				.devices
				.stream()
				.map(device -> new DeviceResource.DeviceDto(device.id, device.name, device.publickey, device.access.stream().map(access -> {
					var vault = access.vault;
					return new VaultResource.VaultDto(vault.id, vault.name, null, null, null);
				}).collect(Collectors.toSet())))
				.collect(Collectors.toSet());
		return Response.ok(new UserDto(user.id, user.name, devices)).build();
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
					.map(device -> new DeviceResource.DeviceDto(device.id, device.name, device.publickey, device
							.access
							.stream()
							.map(access -> new VaultResource.VaultDto(access.id.getVaultId(), null, null, null, null))
							.collect(Collectors.toSet())))
					.collect(Collectors.toSet());
			return new UserDto(user.id, user.name, devices);
		}).collect(Collectors.toList());
		return Response.ok(users).build();
	}

	public static class UserDto {

		private final String id;
		private final String name;
		private final Set<DeviceResource.DeviceDto> devices;

		public UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {
			this.id = id;
			this.name = name;
			this.devices = devices;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<DeviceResource.DeviceDto> getDevices() {
			return devices;
		}
	}
}