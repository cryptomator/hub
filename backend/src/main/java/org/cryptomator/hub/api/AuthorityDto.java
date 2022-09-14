package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

abstract sealed class AuthorityDto permits UserDto, GroupDto {

	private static final String DEFAULT_USER_DATA_IMAGE = """
			data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGNsYXNzPSJoLTUgdy01IiB2aWV3Qm94PSIwIDAgMjAgMjAiIGZpbGw9ImN1cnJlbnRDb2xvciI+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkd\
			Gg9IjIwIiBoZWlnaHQ9IjIwIiByeD0iMDAiIHJ5PSIwMCIgZmlsbD0id2hpdGUiIC8+CiAgPHBhdGggZmlsbC1ydWxlPSJldmVub2RkIiBkPSJNMTAgOWEzIDMgMCAxMDAtNiAzIDMgMCAwMDAgNnptLTcgOWE3IDcgMCAxMTE0IDBIM3oiIGNsaXAt\
			cnVsZT0iZXZlbm9kZCIgLz4KPC9zdmc+
			""";
	private static final String DEFAULT_GROUP_DATA_IMAGE = """
			data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGNsYXNzTmFtZT0iaC01IHctNSIgdmlld0JveD0iMCAwIDIwIDIwIiBmaWxsPSJjdXJyZW50Q29sb3IiPgogIDxyZWN0IHg9IjAiIHk9IjAiI\
			HdpZHRoPSIyMCIgaGVpZ2h0PSIyMCIgcng9IjAwIiByeT0iMDAiIGZpbGw9IndoaXRlIiAvPgogIDxwYXRoIGQ9Ik0xMyA2YTMgMyAwIDExLTYgMCAzIDMgMCAwMTYgMHpNMTggOGEyIDIgMCAxMS00IDAgMiAyIDAgMDE0IDB6TTE0IDE1YTQgNCAw\
			IDAwLTggMHYzaDh2LTN6TTYgOGEyIDIgMCAxMS00IDAgMiAyIDAgMDE0IDB6TTE2IDE4di0zYTUuOTcyIDUuOTcyIDAgMDAtLjc1LTIuOTA2QTMuMDA1IDMuMDA1IDAgMDExOSAxNXYzaC0zek00Ljc1IDEyLjA5NEE1Ljk3MyA1Ljk3MyAwIDAwNCA\
			xNXYzSDF2LTNhMyAzIDAgMDEzLjc1LTIuOTA2eiIgLz4KPC9zdmc+
			""";

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
			case USER -> DEFAULT_USER_DATA_IMAGE;
			case GROUP -> DEFAULT_GROUP_DATA_IMAGE;
		});
	}

}
