package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.VaultAccess;

public final class MemberDto extends AuthorityDto {

	@JsonProperty("ecdhPublicKey")
	public final String ecdhPublicKey;
	@JsonProperty("ecdsaPublicKey")
	public final String ecdsaPublicKey;
	@JsonProperty("role")
	public final VaultAccess.Role role;
	@JsonProperty("memberSize")
	public final Integer memberSize;

	MemberDto(@JsonProperty("id") String id, @JsonProperty("type") Type type, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("ecdhPublicKey") String ecdhPublicKey, @JsonProperty("ecdsaPublicKey") String ecdsaPublicKey, @JsonProperty("role") VaultAccess.Role role, @JsonProperty("memberSize") Integer memberSize) {
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
		return new MemberDto(group.getId(), Type.GROUP, group.getName(), null, null, null, role, group.getMemberSize());
	}

}