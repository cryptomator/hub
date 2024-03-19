package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.cryptomator.hub.entities.VaultAccess;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class VaultMemberAddedEventRepository implements PanacheRepository<VaultMemberAddedEvent> {


	public void log(String addedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
		var event = new VaultMemberAddedEvent();
		event.setTimestamp(Instant.now());
		event.setAddedBy(addedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		event.setRole(role);
		persist(event);
	}
}
