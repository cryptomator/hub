package org.cryptomator.hub.api;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SyncerConfig {

	@ConfigProperty(name = "hub.keycloak.syncer-username")
	String username;

	@ConfigProperty(name = "hub.keycloak.syncer-password")
	String password;

	@ConfigProperty(name = "hub.keycloak.syncer-client-id")
	String keycloakClientId;

	@ConfigProperty(name = "hub.keycloak.public-url")
	String keycloakUrl;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getKeycloakClientId() {
		return keycloakClientId;
	}

	public String getKeycloakUrl() {
		return keycloakUrl;
	}

	public String getKeycloakRealm() {
		return keycloakRealm;
	}

}
