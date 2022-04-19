package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract sealed class AuthorityDto permits UsersResource.UserDto, GroupResource.GroupDto {

	public enum Type {
		USER, GROUP
	}

	@JsonProperty("id")
	public final String id;

	@JsonProperty("type")
	public final Enum<Type> type;

	@JsonProperty("name")
	public final String name;

	protected AuthorityDto(String id, Enum<Type> type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
	}

}
