package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;

public final class GroupDto extends AuthorityDto {

	@JsonProperty("memberSize")
	public final Integer memberSize;

	GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("memberSize") Integer memberSize) {
		super(id, Type.GROUP, name, pictureUrl);
		this.memberSize = memberSize;
	}

	public static GroupDto fromEntity(Group group) {
		return fromEntity(group, false);
	}

	public static GroupDto fromEntity(Group group, boolean withMemberSize) {
		Integer memberSize = withMemberSize ? group.getMemberSize() : null;
		return new GroupDto(group.getId(), group.getName(), group.getPictureUrl(), memberSize);
	}
}
