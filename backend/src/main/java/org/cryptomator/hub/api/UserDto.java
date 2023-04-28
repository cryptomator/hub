package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;

import java.util.Set;

public final class UserDto extends AuthorityDto {

	@JsonProperty("email")
	public final String email;
	@JsonProperty("devices")
	public final Set<DeviceResource.DeviceDto> devices;
	@JsonProperty("accessibleVaults")
	public final Set<VaultResource.VaultDto> accessibleVaults;
	@JsonProperty("publicKey")
	public final String publicKey;
	@JsonProperty("privateKey")
	public final String privateKey;
	@JsonProperty("salt")
	public final String salt;
	@JsonProperty("iterations")
	public final int iterations;

	UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices, @JsonProperty("accessibleVaults") Set<VaultResource.VaultDto> accessibleVaults,
			@JsonProperty("publicKey") @OnlyBase64Chars String publicKey, @JsonProperty("privateKey") @OnlyBase64Chars String privateKey, @JsonProperty("salt") @OnlyBase64Chars String salt, @JsonProperty("iterations") int iterations) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.devices = devices;
		this.accessibleVaults = accessibleVaults;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.salt = salt;
		this.iterations = iterations;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of(), Set.of(), user.publicKey, null, null, 0);
	}
}
