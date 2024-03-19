package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultUpdatedEventRepository implements PanacheRepository<VaultUpdatedEvent> {

	public void log(String updatedBy, UUID vaultId, String vaultName, String vaultDescription, boolean vaultArchived) {
		var event = new VaultUpdatedEvent();
		event.setTimestamp(Instant.now());
		event.setUpdatedBy(updatedBy);
		event.setVaultId(vaultId);
		event.setVaultName(vaultName);
		event.setVaultDescription(vaultDescription);
		event.setVaultArchived(vaultArchived);
		persist(event);
	}

}
