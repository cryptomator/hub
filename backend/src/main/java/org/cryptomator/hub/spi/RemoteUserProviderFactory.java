package org.cryptomator.hub.spi;

import org.cryptomator.hub.spi.keycloak.KeycloakRemoteUserProvider;

public class RemoteUserProviderFactory {

	public static RemoteUserProvider get(ConfigResource configResource) {
		return new KeycloakRemoteUserProvider(configResource);
	}

}
