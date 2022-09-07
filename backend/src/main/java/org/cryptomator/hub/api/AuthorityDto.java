package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

abstract sealed class AuthorityDto permits UserDto, GroupDto {

	public enum Type {
		USER, GROUP
	}

	@JsonProperty("id")
	public final String id;

	@JsonProperty("type")
	public final Type type;

	@JsonProperty("name")
	public final String name;

	@JsonProperty("pictureUrl")
	public final String pictureUrl;

	protected AuthorityDto(String id, Type type, String name, String pictureUrl) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.pictureUrl = Objects.requireNonNullElseGet(pictureUrl, () -> switch (type) {
			case USER -> "/user-icon.svg";
			case GROUP -> "/group-icon.svg";
		});
	}

}
