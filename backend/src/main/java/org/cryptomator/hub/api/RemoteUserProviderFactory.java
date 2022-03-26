package org.cryptomator.hub.api;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RemoteUserProviderFactory {

	public RemoteUserProvider get(SyncerConfig syncerConfig) {
		return new KeycloakRemoteUserProvider(syncerConfig);
	}

}
