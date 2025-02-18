package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import jakarta.inject.Inject;

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

	@Inject
	static User.Repository userRepo; // Inject User Repository für die neue Berechnung

	static AuthorityDto fromEntity(Authority a) {
		return switch (a) {
			case User u -> UserDto.justPublicInfo(u);
			case Group g -> new GroupDto(g.getId(), g.getName(), (int) userRepo.getEffectiveGroupUsers(g.getId()).count()); 
			default -> throw new IllegalStateException("authority is not of type user or group");
		};
	}

}
