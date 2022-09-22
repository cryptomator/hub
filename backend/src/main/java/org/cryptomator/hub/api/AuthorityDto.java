package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

abstract sealed class AuthorityDto permits UserDto, GroupDto {

	private static final String DEFAULT_USER_IMAGE_URI = """
			data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0iYmxhY2siIGNsYXNzPSJ3LT\
			UgaC01Ij4gICA8Y2lyY2xlIGN4PSIxMCIgY3k9IjEwIiByPSIxMCIgZmlsbD0id2hpdGUiIC8+ICAgPHBhdGggZD0iTTEwIDhhMyAzIDAgMTAwLTYgMyAzIDAgMDAwIDZ6TTMuND\
			Y1IDE0LjQ5M2ExLjIzIDEuMjMgMCAwMC40MSAxLjQxMkE5Ljk1NyA5Ljk1NyAwIDAwMTAgMThjMi4zMSAwIDQuNDM4LS43ODQgNi4xMzEtMi4xLjQzLS4zMzMuNjA0LS45MDMuND\
			A4LTEuNDFhNy4wMDIgNy4wMDIgMCAwMC0xMy4wNzQuMDAzeiIgLz4gPC9zdmc+
			""";
	private static final String DEFAULT_GROUP_IMAGE_URI = """
			data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0iYmxhY2siIGNsYXNzPSJ3LT\
			UgaC01Ij4gICA8Y2lyY2xlIGN4PSIxMCIgY3k9IjEwIiByPSIxMCIgZmlsbD0id2hpdGUiIC8+ICAgPHBhdGggZD0iTTEwIDlhMyAzIDAgMTAwLTYgMyAzIDAgMDAwIDZ6TTYgOG\
			EyIDIgMCAxMS00IDAgMiAyIDAgMDE0IDB6TTEuNDkgMTUuMzI2YS43OC43OCAwIDAxLS4zNTgtLjQ0MiAzIDMgMCAwMTQuMzA4LTMuNTE2IDYuNDg0IDYuNDg0IDAgMDAtMS45MD\
			UgMy45NTljLS4wMjMuMjIyLS4wMTQuNDQyLjAyNS42NTRhNC45NyA0Ljk3IDAgMDEtMi4wNy0uNjU1ek0xNi40NCAxNS45OGE0Ljk3IDQuOTcgMCAwMDIuMDctLjY1NC43OC43OC\
			AwIDAwLjM1Ny0uNDQyIDMgMyAwIDAwLTQuMzA4LTMuNTE3IDYuNDg0IDYuNDg0IDAgMDExLjkwNyAzLjk2IDIuMzIgMi4zMiAwIDAxLS4wMjYuNjU0ek0xOCA4YTIgMiAwIDExLT\
			QgMCAyIDIgMCAwMTQgMHpNNS4zMDQgMTYuMTlhLjg0NC44NDQgMCAwMS0uMjc3LS43MSA1IDUgMCAwMTkuOTQ3IDAgLjg0My44NDMgMCAwMS0uMjc3LjcxQTYuOTc1IDYuOTc1ID\
			AgMDExMCAxOGE2Ljk3NCA2Ljk3NCAwIDAxLTQuNjk2LTEuODF6IiAvPiA8L3N2Zz4=
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
			case USER -> DEFAULT_USER_IMAGE_URI;
			case GROUP -> DEFAULT_GROUP_IMAGE_URI;
		});
	}

}
