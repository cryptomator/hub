package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
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
	String claimedBy;

	@Column(name = "vault_id")
	UUID vaultId;

	public String getClaimedBy() {
		return claimedBy;
	}

	public void setClaimedBy(String claimedBy) {
		this.claimedBy = claimedBy;
	}

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

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

	@ApplicationScoped
	public static class Repository implements PanacheRepository<VaultOwnershipClaimedEvent> {

		public void log(String claimedBy, UUID vaultId) {
			var event = new VaultOwnershipClaimedEvent();
			event.setTimestamp(Instant.now());
			event.setClaimedBy(claimedBy);
			event.setVaultId(vaultId);
			persist(event);
		}
	}
}
