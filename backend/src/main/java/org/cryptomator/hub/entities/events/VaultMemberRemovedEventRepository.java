package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultMemberRemovedEventRepository implements PanacheRepository<VaultMemberRemovedEvent> {

	public void log(String removedBy, UUID vaultId, String authorityId) {
		var event = new VaultMemberRemovedEvent();
		event.setTimestamp(Instant.now());
		event.setRemovedBy(removedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		persist(event);
	}

}
