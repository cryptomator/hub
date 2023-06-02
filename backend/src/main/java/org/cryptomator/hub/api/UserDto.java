package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;

import java.util.Set;

public final class UserDto extends AuthorityDto {

	@JsonProperty("email")
	public final String email;
	@JsonProperty("devices")
	public final Set<DeviceResource.DeviceDto> devices;
	@JsonProperty("publicKey")
	public final String publicKey;
	@JsonProperty("privateKey")
	public final String privateKey;
	@JsonProperty("setupCode")
	public final String setupCode;

	UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			@Nullable @JsonProperty("publicKey") @OnlyBase64Chars String publicKey, @Nullable @JsonProperty("privateKey") @ValidJWE String privateKey, @Nullable @JsonProperty("setupCode") @ValidJWE String setupCode) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.devices = devices;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.setupCode = setupCode;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of(), user.publicKey, null, null);
	}
}
