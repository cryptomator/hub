package org.cryptomator.hub.keycloak;

import org.jspecify.annotations.Nullable;

public record KeycloakUserDto(String id, String name, @Nullable String email, @Nullable String firstName, @Nullable String lastName, @Nullable String pictureUrl, boolean enabled) {
}
