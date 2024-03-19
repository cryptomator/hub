package org.cryptomator.hub.entities.events;

import jakarta.enterprise.context.ApplicationScoped;
import org.cryptomator.hub.entities.VaultAccess;

import java.time.Instant;
import java.util.UUID;

import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.persist;

@ApplicationScoped
public class VaultMemberUpdatedEventRepository {

	public void log(String updatedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
		var event = new VaultMemberUpdatedEvent();
		event.setTimestamp(Instant.now());
		event.setUpdatedBy(updatedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		event.setRole(role);
		persist(event);
	}

}
