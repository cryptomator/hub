package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class UserDto extends AuthorityDto {

	private final String email;
	private final @Nullable String firstName;
	private final @Nullable String lastName;
	private final @Nullable String language;
	private final boolean enabled;
	private final Set<DeviceResource.DeviceDto> devices;
	private final @Nullable String ecdhPublicKey;
	private final @Nullable String ecdsaPublicKey;
	private final @Nullable String privateKeys;
	private final @Nullable String setupCode;

	@JsonCreator
	public UserDto(
			@JsonProperty("id") @NotNull String id,
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("pictureUrl") @Nullable String pictureUrl,
			@JsonProperty("email") @NotNull String email,
			@JsonProperty("firstName") @Nullable String firstName,
			@JsonProperty("lastName") @Nullable String lastName,
			@JsonProperty("language") @Nullable String language,
			@JsonProperty("enabled") boolean enabled,
			@JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			// Accept either "ecdhPublicKey" or the legacy "publicKey" on input
			@Nullable @JsonProperty("ecdhPublicKey") @OnlyBase64Chars String ecdhPublicKey,
			@Nullable @JsonProperty("publicKey") @OnlyBase64Chars String publicKey,
			@Nullable @JsonProperty("ecdsaPublicKey") @OnlyBase64Chars String ecdsaPublicKey,
			// Accept either "privateKeys" or the legacy "privateKey" on input
			@Nullable @JsonProperty("privateKeys") @ValidJWE String privateKeys,
			@Nullable @JsonProperty("privateKey") @ValidJWE String privateKey,
			@Nullable @JsonProperty("setupCode") @ValidJWE String setupCode) {
		super(id, Type.USER, name, pictureUrl);
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.language = language;
		this.enabled = enabled;
		this.devices = devices;
		this.ecdhPublicKey = ecdhPublicKey != null ? ecdhPublicKey : publicKey;
		this.ecdsaPublicKey = ecdsaPublicKey;
		this.privateKeys = privateKeys != null ? privateKeys : privateKey;
		this.setupCode = setupCode;
	}

	public UserDto(
			String id,
			String name,
			@Nullable String pictureUrl,
			String email,
			@Nullable String firstName,
			@Nullable String lastName,
			@Nullable String language,
			boolean enabled,
			Set<DeviceResource.DeviceDto> devices,
			@Nullable String ecdhPublicKey,
			@Nullable String ecdsaPublicKey,
			@Nullable String privateKeys,
			@Nullable String setupCode) {
		this(id, name, pictureUrl, email, firstName, lastName, language, enabled, devices, ecdhPublicKey, ecdhPublicKey, ecdsaPublicKey, privateKeys, privateKeys, setupCode);
	}

	@JsonProperty("email")
	public String getEmail() {
		return email;
	}

	@JsonProperty("firstName")
	public @Nullable String getFirstName() {
		return firstName;
	}

	@JsonProperty("lastName")
	public @Nullable String getLastName() {
		return lastName;
	}

	@JsonProperty("language")
	public @Nullable String getLanguage() {
		return language;
	}

	@JsonProperty("enabled")
	public boolean isEnabled() {
		return enabled;
	}

	@JsonProperty("devices")
	public Set<DeviceResource.DeviceDto> getDevices() {
		return devices;
	}

	@JsonProperty("ecdhPublicKey")
	public @Nullable String getEcdhPublicKey() {
		return ecdhPublicKey;
	}

	/**
	 * Same as {@link #ecdhPublicKey}, kept for compatibility purposes
	 *
	 * @deprecated to be removed in Hub 2.0.0, tracked in <a href="https://github.com/cryptomator/hub/issues/316">#316</a>
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("publicKey")
	public @Nullable String getPublicKey() {
		return ecdhPublicKey;
	}

	@JsonProperty("ecdsaPublicKey")
	public @Nullable String getEcdsaPublicKey() {
		return ecdsaPublicKey;
	}

	@JsonProperty("privateKeys")
	public @Nullable String getPrivateKeys() {
		return privateKeys;
	}

	/**
	 * Same as {@link #privateKeys}, kept for compatibility purposes
	 *
	 * @deprecated to be removed in Hub 2.0.0, tracked in <a href="https://github.com/cryptomator/hub/issues/316">#316</a>
	 */
	@Deprecated(forRemoval = true)
	@JsonProperty("privateKey")
	public @Nullable String getPrivateKey() {
		return privateKeys;
	}

	@JsonProperty("setupCode")
	public @Nullable String getSetupCode() {
		return setupCode;
	}

	public static UserDto justPublicInfo(User user) {
		return new UserDto(
				user.getId(),
				user.getName(),
				user.getPictureUrl(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getLanguage(),
				user.isEnabled(),
				Set.of(),
				user.getEcdhPublicKey(),
				user.getEcdsaPublicKey(),
				null,
				null);
	}

	public WithCounts withCounts(long groupsCount, long vaultsCount, long devicesCount) {
		return new WithCounts(
				this,
				devicesCount,
				groupsCount,
				vaultsCount);
	}

	public WithDetails withDetails(List<GroupDto> groups, List<VaultResource.VaultDtoWithRole> accessibleVaults, Set<DeviceResource.DeviceDto> devices, Set<DeviceResource.DeviceDto> legacyDevices, Set<String> realmRoles) {
		return new WithDetails(
				this,
				groups,
				accessibleVaults,
				devices,
				legacyDevices,
				realmRoles);
	}

	public record WithCounts(
			@JsonUnwrapped UserDto user,
			@JsonProperty("devicesCount") long devicesCount,
			@JsonProperty("groupsCount") long groupsCount,
			@JsonProperty("accessibleVaultCount") long accessibleVaultCount
	) {
	}

	public record WithDetails(
			@JsonUnwrapped UserDto user,
			@JsonProperty("groups") List<GroupDto> groups,
			@JsonProperty("accessibleVaults") List<VaultResource.VaultDtoWithRole> accessibleVaults,
			@JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			@JsonProperty("legacyDevices") Set<DeviceResource.DeviceDto> legacyDevices,
			@JsonProperty("realmRoles") Set<String> realmRoles
	) {
	}
}
