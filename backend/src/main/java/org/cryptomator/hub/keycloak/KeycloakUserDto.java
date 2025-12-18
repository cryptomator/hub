package org.cryptomator.hub.keycloak;

public record KeycloakUserDto(String id, String name, String email, String firstName, String lastName, String pictureUrl) { }
