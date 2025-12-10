package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;

public final class GroupDto extends AuthorityDto {

	@JsonProperty("memberSize")
	public final Integer memberSize;

	@JsonProperty("vaultCount")
	public final Integer vaultCount;

	GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("memberSize") Integer memberSize, @JsonProperty("vaultCount") Integer vaultCount) {
		super(id, Type.GROUP, name, null);
		this.memberSize = memberSize;
		this.vaultCount = vaultCount;
	}

	public static GroupDto fromEntity(Group group) {
		return new GroupDto(group.getId(), group.getName(), null, null);
	}

	public static GroupDto fromEntity(Group group, boolean withMemberSize) {
		return new GroupDto(group.getId(), group.getName(), withMemberSize ? group.getMemberSize() : null, null);
	}

	public static GroupDto fromEntity(Group group, Integer memberSize, Integer vaultCount) {
		return new GroupDto(group.getId(), group.getName(), memberSize, vaultCount);
	}
}
