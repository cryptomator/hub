package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;

import java.util.Set;

public final class UserDto extends AuthorityDto {

	@JsonProperty("email")
	public final String email;
	@JsonProperty("language")
	public final String language;
	@JsonProperty("devices")
	public final Set<DeviceResource.DeviceDto> devices;
	@JsonProperty("ecdhPublicKey")
	@JsonAlias("publicKey") // name for history reasons (don't break client compatibility)
	public final String ecdhPublicKey;
	@JsonProperty("ecdsaPublicKey")
	public final String ecdsaPublicKey;
	@JsonProperty("privateKeys")
	@JsonAlias("privateKey") // name for history reasons (don't break client compatibility)
	public final String privateKeys;
	@JsonProperty("setupCode")
	public final String setupCode;

	public UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("language") String language, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
				   @Nullable @JsonProperty("ecdhPublicKey") @JsonAlias("publicKey") @OnlyBase64Chars String ecdhPublicKey, @Nullable @JsonProperty("ecdsaPublicKey") @OnlyBase64Chars String ecdsaPublicKey, @Nullable @JsonProperty("privateKeys") @JsonAlias("privateKey") @ValidJWE String privateKeys, @Nullable @JsonProperty("setupCode") @ValidJWE String setupCode) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.language = language;
		this.devices = devices;
		this.ecdhPublicKey = ecdhPublicKey;
		this.ecdsaPublicKey = ecdsaPublicKey;
		this.privateKeys = privateKeys;
		this.setupCode = setupCode;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), user.getLanguage(), Set.of(), user.getEcdhPublicKey(), user.getEcdsaPublicKey(), null, null);
	}

	/**
	 * Same as {@link #ecdhPublicKey}, kept for compatibility purposes
	 * @deprecated to be removed when all clients moved to the new DTO field names
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("publicKey")
	public String getPublicKey() {
		return ecdhPublicKey;
	}

	/**
	 * Same as {@link #privateKeys}, kept for compatibility purposes
	 * @deprecated to be removed when all clients moved to the new DTO field names
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("privateKey")
	public String getPrivateKey() {
		return privateKeys;
	}
}
