package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.UserDao;
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

	@Inject
	UserDao userDao;

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@NoCache
	public String me() {
		return userDao.get(userInfo.getString("sub")).getName();
	}

	@GET
	@Path("/devices")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllIncludingDevices() {
		var users = userDao.getAllWithDevices().stream().map(user -> {
			var devices = user
					.getDevices()
					.stream()
					.map(device -> new DeviceResource.DeviceDto(device.getId(), device.getName(), device.getPublickey()))
					.collect(Collectors.toSet());
			return new UserDto(user.getId(), user.getName(), devices);
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