package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;

import java.util.Set;

public final class UserDto extends AuthorityDto {

	private final String email;
	private final String language;
	private final Set<DeviceResource.DeviceDto> devices;
	private final String ecdhPublicKey;
	private final String ecdsaPublicKey;
	private final String privateKeys;
	private final String setupCode;

	@JsonCreator
	public UserDto(
			@JsonProperty("id") String id,
			@JsonProperty("name") String name,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("email") String email,
			@JsonProperty("language") String language,
			@JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			// Accept either "ecdhPublicKey" or the legacy "publicKey" on input
			@Nullable
			@JsonProperty("ecdhPublicKey") @OnlyBase64Chars String ecdhPublicKey,
			@Nullable
			@JsonProperty("publicKey") @OnlyBase64Chars String publicKey,
			@Nullable
			@JsonProperty("ecdsaPublicKey") @OnlyBase64Chars String ecdsaPublicKey,
			// Accept either "privateKeys" or the legacy "privateKey" on input
			@Nullable
			@JsonProperty("privateKeys") @ValidJWE String privateKeys,
			@Nullable
			@JsonProperty("privateKey") @ValidJWE String privateKey,
			@Nullable
			@JsonProperty("setupCode") @ValidJWE String setupCode) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.language = language;
		this.devices = devices;
		if (ecdhPublicKey != null) {
			this.ecdhPublicKey = ecdhPublicKey;
		} else {
			this.ecdhPublicKey = publicKey;
		}
		this.ecdsaPublicKey = ecdsaPublicKey;
		if (privateKeys != null) {
			this.privateKeys = privateKeys;
		} else {
			this.privateKeys = privateKey;
		}
		this.setupCode = setupCode;
	}

	@JsonProperty("email")
	public String getEmail() {
		return email;
	}

	@JsonProperty("language")
	public String getLanguage() {
		return language;
	}

	@JsonProperty("devices")
	public Set<DeviceResource.DeviceDto> getDevices() {
		return devices;
	}

	@JsonProperty("ecdhPublicKey")
	public String getEcdhPublicKey() {
		return ecdhPublicKey;
	}

	/**
	 * Same as {@link #ecdhPublicKey}, kept for compatibility purposes
	 * @deprecated to be removed in Hub 2.0.0, tracked in <a href="https://github.com/cryptomator/hub/issues/316">#316</a>
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("publicKey")
	public String getPublicKey() {
		return ecdhPublicKey;
	}

	@JsonProperty("ecdsaPublicKey")
	public String getEcdsaPublicKey() {
		return ecdsaPublicKey;
	}

	@JsonProperty("privateKeys")
	public String getPrivateKeys() {
		return privateKeys;
	}

	/**
	 * Same as {@link #privateKeys}, kept for compatibility purposes
	 * @deprecated to be removed in Hub 2.0.0, tracked in <a href="https://github.com/cryptomator/hub/issues/316">#316</a>
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("privateKey")
	public String getPrivateKey() {
		return privateKeys;
	}

	@JsonProperty("setupCode")
	public String getSetupCode() {
		return setupCode;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(
				user.getId(),
				user.getName(),
				user.getPictureUrl(),
				user.getEmail(),
				user.getLanguage(),
				Set.of(),
				user.getEcdhPublicKey(),
				user.getEcdhPublicKey(),
				user.getEcdsaPublicKey(),
				null,
				null,
				null);
	}
}
