package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.VaultAccess;

public final class MemberDto extends AuthorityDto {

	@JsonProperty("role")
	public final VaultAccess.Role role;

	MemberDto(@JsonProperty("id") String id, @JsonProperty("type") AuthorityDto.Type type, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("role") VaultAccess.Role role) {
		super(id, type, name, pictureUrl);
		this.role = role;
	}

	public static MemberDto fromEntity(User user, VaultAccess.Role role) {
		return new MemberDto(user.id, Type.USER, user.name, user.pictureUrl, role);
	}

	public static MemberDto fromEntity(Group group, VaultAccess.Role role) {
		return new MemberDto(group.id, Type.GROUP, group.name, null, role);
	}

}
