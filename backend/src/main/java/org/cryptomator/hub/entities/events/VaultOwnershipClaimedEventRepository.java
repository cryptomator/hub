package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultOwnershipClaimedEventRepository implements PanacheRepository<VaultOwnershipClaimedEvent> {

	public void log(String claimedBy, UUID vaultId) {
		var event = new VaultOwnershipClaimedEvent();
		event.setTimestamp(Instant.now());
		event.setClaimedBy(claimedBy);
		event.setVaultId(vaultId);
		persist(event);
	}

}
