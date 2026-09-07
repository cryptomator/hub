package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.VaultAccess;
import org.jspecify.annotations.Nullable;

public final class MemberDto extends AuthorityDto {

	@JsonProperty("ecdhPublicKey")
	public final @Nullable String ecdhPublicKey;
	@JsonProperty("ecdsaPublicKey")
	public final @Nullable String ecdsaPublicKey;
	@JsonProperty("vaultRole")
	public final VaultAccess.Role role;
	@JsonProperty("memberSize")
	public final @Nullable Integer memberSize;

	MemberDto(@JsonProperty("id") String id, @JsonProperty("type") Type type, @JsonProperty("name") String name, @JsonProperty("pictureUrl") @Nullable String pictureUrl, @JsonProperty("ecdhPublicKey") @Nullable String ecdhPublicKey, @JsonProperty("ecdsaPublicKey") @Nullable String ecdsaPublicKey, @JsonProperty("vaultRole") VaultAccess.Role role, @JsonProperty("memberSize") @Nullable Integer memberSize) {
		super(id, type, name, pictureUrl);
		this.ecdhPublicKey = ecdhPublicKey;
		this.ecdsaPublicKey = ecdsaPublicKey;
		this.role = role;
		this.memberSize = memberSize;
	}

	public static MemberDto fromEntity(User user, VaultAccess.Role role) {
		return new MemberDto(user.getId(), Type.USER, user.getName(), user.getPictureUrl(), user.getEcdhPublicKey(), user.getEcdsaPublicKey(), role, null);
	}

	public static MemberDto fromEntity(Group group, VaultAccess.Role role) {
		return new MemberDto(group.getId(), Type.GROUP, group.getName(), group.getPictureUrl(), null, null, role, group.getMemberSize());
	}

}