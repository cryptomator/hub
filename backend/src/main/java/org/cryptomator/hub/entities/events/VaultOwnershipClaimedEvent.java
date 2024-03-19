package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_ownership_claim")
@DiscriminatorValue(VaultOwnershipClaimedEvent.TYPE)
public class VaultOwnershipClaimedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_OWNERSHIP_CLAIM";

	@Column(name = "claimed_by")
	public String claimedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultOwnershipClaimedEvent that = (VaultOwnershipClaimedEvent) o;
		return super.equals(that) //
				&& Objects.equals(claimedBy, that.claimedBy) //
				&& Objects.equals(vaultId, that.vaultId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, claimedBy, vaultId);
	}

	public static void log(String claimedBy, UUID vaultId) {
		var event = new VaultOwnershipClaimedEvent();
		event.timestamp = Instant.now();
		event.claimedBy = claimedBy;
		event.vaultId = vaultId;
		event.persist();
	}

}
