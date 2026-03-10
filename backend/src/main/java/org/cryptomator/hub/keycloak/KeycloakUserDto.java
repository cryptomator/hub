package org.cryptomator.hub.keycloak;

import java.util.Set;

public record KeycloakUserDto(String id, String name, String email, String firstName, String lastName, String pictureUrl, Set<RealmRole> roles) {
}
