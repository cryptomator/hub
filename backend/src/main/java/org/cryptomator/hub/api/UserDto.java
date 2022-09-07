package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.User;

import java.util.Set;

public final class UserDto extends AuthorityDto {

	@JsonProperty("email")
	public final String email;
	@JsonProperty("devices")
	public final Set<DeviceResource.DeviceDto> devices;

	UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.devices = devices;
	}

	public static UserDto fromEntity(User user) {
		return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of());
	}
}
