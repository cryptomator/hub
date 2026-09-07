package org.cryptomator.hub.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.EnumMap;

@ApplicationScoped
public class KeycloakRealmRoles {

	private final Keycloak keycloak;
	private final String keycloakRealm;
	private final EnumMap<RealmRole, RoleRepresentation> cachedRoles = new EnumMap<>(RealmRole.class);

	@Inject
	KeycloakRealmRoles(Keycloak keycloak, @ConfigProperty(name = "hub.keycloak.realm") String keycloakRealm) {
		this.keycloak = keycloak;
		this.keycloakRealm = keycloakRealm;
	}

	public RoleRepresentation getRealmRole(RealmRole realmRole) {
		return cachedRoles.computeIfAbsent(realmRole, this::fetchRealmRole);
	}

	private RoleRepresentation fetchRealmRole(RealmRole realmRole) {
		return keycloak.realm(keycloakRealm).roles().get(realmRole.kcName()).toRepresentation();
	}
}
