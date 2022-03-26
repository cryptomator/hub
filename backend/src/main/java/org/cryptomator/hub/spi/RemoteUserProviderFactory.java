package org.cryptomator.hub.spi;

import org.cryptomator.hub.spi.keycloak.KeycloakRemoteUserProvider;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RemoteUserProviderFactory {

	public RemoteUserProvider get(SyncerConfig syncerConfig) {
		return new KeycloakRemoteUserProvider(syncerConfig);
	}

}
