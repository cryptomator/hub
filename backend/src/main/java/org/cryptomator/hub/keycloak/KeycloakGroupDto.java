package org.cryptomator.hub.keycloak;

import org.jspecify.annotations.Nullable;

import java.util.Set;

public record KeycloakGroupDto(String id, String name, @Nullable String pictureUrl, Set<KeycloakUserDto> members) {
}
