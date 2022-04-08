package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Authority;

abstract sealed class AuthorityDto permits UsersResource.UserDto, GroupResource.GroupDto {

	@JsonProperty("id")
	public final String id;

	@JsonProperty("type")
	public final String type;

	@JsonProperty("name")
	public final String name;

	protected AuthorityDto(String id, String type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
	}

}
