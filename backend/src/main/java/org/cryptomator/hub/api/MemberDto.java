package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.VaultAccess;

public final class MemberDto extends AuthorityDto {

	@JsonProperty("publicKey")
	public final String publicKey;
	@JsonProperty("role")
	public final VaultAccess.Role role;

	MemberDto(@JsonProperty("id") String id, @JsonProperty("type") Type type, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("publicKey") String publicKey, @JsonProperty("role") VaultAccess.Role role) {
		super(id, type, name, pictureUrl);
		this.publicKey = publicKey;
		this.role = role;
	}

	public static MemberDto fromEntity(User user, VaultAccess.Role role) {
		return new MemberDto(user.getId(), Type.USER, user.getName(), user.getPictureUrl(), user.getPublicKey(), role);
	}

	public static MemberDto fromEntity(Group group, VaultAccess.Role role) {
		return new MemberDto(group.getId(), Type.GROUP, group.getName(), null, null, role);
	}

}
