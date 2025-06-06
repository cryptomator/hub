package org.cryptomator.hub.keycloak;

import java.util.Set;

public record KeycloakGroupDto(String id, String name, Set<KeycloakUserDto> members) { }
