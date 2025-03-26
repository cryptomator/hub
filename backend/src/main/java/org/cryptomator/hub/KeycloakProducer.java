package org.cryptomator.hub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@ApplicationScoped
public class KeycloakProducer {

	@ConfigProperty(name = "hub.keycloak.syncer-client-id")
	String clientId;

	@ConfigProperty(name = "hub.keycloak.syncer-client-secret")
	String clientSecret;

	@ConfigProperty(name = "hub.keycloak.local-url")
	String url;

	@ConfigProperty(name = "hub.keycloak.realm")
	String realm;

	@Produces
	@ApplicationScoped
	public Keycloak produceKeycloak() {
		return KeycloakBuilder.builder()
				.serverUrl(url)
				.realm(realm)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.build();
	}

	public void disposeKeycloak(@Disposes Keycloak keycloak) {
		keycloak.close();
	}
}
