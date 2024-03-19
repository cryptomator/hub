package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultCreatedEventRepository implements PanacheRepository<VaultCreatedEvent> {

	public void log(String createdBy, UUID vaultId, String vaultName, String vaultDescription) {
		var event = new VaultCreatedEvent();
		event.setTimestamp(Instant.now());
		event.setCreatedBy(createdBy);
		event.setVaultId(vaultId);
		event.setVaultName(vaultName);
		event.setVaultDescription(vaultDescription);
		persist(event);
	}

}
