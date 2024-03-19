package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultKeyRetrievedEventRepository implements PanacheRepository<VaultKeyRetrievedEvent> {

	public void log(String retrievedBy, UUID vaultId, VaultKeyRetrievedEvent.Result result) {
		var event = new VaultKeyRetrievedEvent();
		event.setTimestamp(Instant.now());
		event.setRetrievedBy(retrievedBy);
		event.setVaultId(vaultId);
		event.setResult(result);
		persist(event);
	}
}
