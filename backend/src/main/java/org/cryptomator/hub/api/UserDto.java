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
	@JsonProperty("ecdhPublicKey")
	public final String ecdhPublicKey;
	@JsonProperty("ecdsaPublicKey")
	public final String ecdsaPublicKey;
	@JsonProperty("privateKey")
	public final String privateKeys;
	@JsonProperty("setupCode")
	public final String setupCode;

	UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			@Nullable @JsonProperty("ecdhPublicKey") @OnlyBase64Chars String ecdhPublicKey, @Nullable @JsonProperty("ecdsaPublicKey") @OnlyBase64Chars String ecdsaPublicKey, @Nullable @JsonProperty("privateKeys") @ValidJWE String privateKeys, @Nullable @JsonProperty("setupCode") @ValidJWE String setupCode) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.devices = devices;
		this.ecdhPublicKey = ecdhPublicKey;
		this.ecdsaPublicKey = ecdsaPublicKey;
		this.privateKeys = privateKeys;
		this.setupCode = setupCode;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), Set.of(), user.getEcdhPublicKey(), user.getEcdsaPublicKey(),null, null);
	}
}
