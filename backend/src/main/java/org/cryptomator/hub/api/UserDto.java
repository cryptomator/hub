package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidJWE;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class UserDto extends AuthorityDto {

	private final String email;
	private final String firstName;
	private final String lastName;
	private final String language;
	private final Set<String> realmRoles;
	private final Set<DeviceResource.DeviceDto> devices;
	private final String ecdhPublicKey;
	private final String ecdsaPublicKey;
	private final String privateKeys;
	private final String setupCode;

	@JsonCreator
	public UserDto(
			@JsonProperty("id") @NotNull String id,
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("email") @NotNull String email,
			@JsonProperty("firstName") String firstName,
			@JsonProperty("lastName") String lastName,
			@JsonProperty("language") String language,
			@JsonProperty("realmRoles") @NotNull Set<String> realmRoles,
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
		this.realmRoles = realmRoles;
		this.devices = devices;
		this.ecdhPublicKey = ecdhPublicKey != null ? ecdhPublicKey : publicKey;
		this.ecdsaPublicKey = ecdsaPublicKey;
		this.privateKeys = privateKeys != null ? privateKeys : privateKey;
		this.setupCode = setupCode;
	}

	public UserDto(
			String id,
			String name,
			String pictureUrl,
			String email,
			String firstName,
			String lastName,
			String language,
			Set<String> realmRoles,
			Set<DeviceResource.DeviceDto> devices,
			String ecdhPublicKey,
			String ecdsaPublicKey,
			String privateKeys,
			String setupCode) {
		this(id, name, pictureUrl, email, firstName, lastName, language, realmRoles, devices, ecdhPublicKey, ecdhPublicKey, ecdsaPublicKey, privateKeys, privateKeys, setupCode);
	}

	@JsonProperty("email")
	public String getEmail() {
		return email;
	}

	@JsonProperty("firstName")
	public String getFirstName() {
		return firstName;
	}

	@JsonProperty("lastName")
	public String getLastName() {
		return lastName;
	}

	@JsonProperty("language")
	public String getLanguage() {
		return language;
	}

	@JsonProperty("realmRoles")
	public Set<String> getRealmRoles() {
		return realmRoles;
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
				user.getFirstName(),
				user.getLastName(),
				user.getLanguage(),
				Set.copyOf(user.getRealmRoles()),
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

	public WithDetails withDetails(List<GroupDto> groups, List<VaultResource.VaultDtoWithRole> accessibleVaults, Set<DeviceResource.DeviceDto> devices, Set<DeviceResource.DeviceDto> legacyDevices) {
			return new WithDetails(
					this,
					groups,
					accessibleVaults,
					devices,
					legacyDevices);
	}

	public record WithCounts(
			@JsonUnwrapped UserDto user,
			@JsonProperty("devicesCount") long devicesCount,
			@JsonProperty("groupsCount") long groupsCount,
			@JsonProperty("vaultsCount") long vaultsCount
	) {}

	public record WithDetails(
			@JsonUnwrapped UserDto user,
			@JsonProperty("groups") List<GroupDto> groups,
			@JsonProperty("accessibleVaults") List<VaultResource.VaultDtoWithRole> accessibleVaults,
			@JsonProperty("devices") Set<DeviceResource.DeviceDto> devices,
			@JsonProperty("legacyDevices") Set<DeviceResource.DeviceDto> legacyDevices
	) {}
}
