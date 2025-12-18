package org.cryptomator.hub.keycloak;

import java.util.Set;

public record KeycloakGroupDto(String id, String name, String pictureUrl, Set<KeycloakUserDto> members) { }
