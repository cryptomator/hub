package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultAccessGrantedEventRepository implements PanacheRepository<VaultAccessGrantedEvent> {

	public void log(String grantedBy, UUID vaultId, String authorityId) {
		var event = new VaultAccessGrantedEvent();
		event.setTimestamp(Instant.now());
		event.setGrantedBy(grantedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		persist(event);
	}

}
