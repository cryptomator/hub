package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;

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
	@JsonProperty("recoveryJwe")
	public final String recoveryJwe;
	@JsonProperty("recoveryPbkdf2")
	public final String recoveryPbkdf2;
	@JsonProperty("recoverySalt")
	public final String recoverySalt;
	@JsonProperty("recoveryIterations")
	public final int recoveryIterations;

	UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email, @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices, @JsonProperty("accessibleVaults") Set<VaultResource.VaultDto> accessibleVaults,
			@JsonProperty("publicKey") @OnlyBase64Chars String publicKey, @JsonProperty("recoveryJwe") @ValidJWE String recoveryJwe, @JsonProperty("recoveryPbkdf2") @OnlyBase64Chars String recoveryPbkdf2, @JsonProperty("recoverySalt") @OnlyBase64Chars String recoverySalt, @JsonProperty("recoveryIterations") int recoveryIterations) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.devices = devices;
		this.accessibleVaults = accessibleVaults;
		this.publicKey = publicKey;
		this.recoveryJwe = recoveryJwe;
		this.recoveryPbkdf2 = recoveryPbkdf2;
		this.recoverySalt = recoverySalt;
		this.recoveryIterations = recoveryIterations;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(user.id, user.name, user.pictureUrl, user.email, Set.of(), Set.of(), user.publicKey,  null,null, null, 0);
	}
}
