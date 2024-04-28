package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;

public final class GroupDto extends AuthorityDto {

	GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name) {
		super(id, Type.GROUP, name, null);
	}

	public static GroupDto fromEntity(Group group) {
		return new GroupDto(group.getId(), group.getName());
	}

}
