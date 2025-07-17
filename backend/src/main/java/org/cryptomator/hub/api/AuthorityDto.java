package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
abstract sealed class AuthorityDto permits UserDto, GroupDto, MemberDto {

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
		this.pictureUrl = pictureUrl;
	}

	static AuthorityDto fromEntity(Authority a) {
		return fromEntity(a, false);
	}

	static AuthorityDto fromEntity(Authority a, boolean withMemberSize) {
		return switch (a) {
			case User u -> UserDto.justPublicInfo(u);
			case Group g -> GroupDto.fromEntity(g, withMemberSize);
			default -> throw new IllegalStateException("authority is not of type user or group");
		};
	}

}
