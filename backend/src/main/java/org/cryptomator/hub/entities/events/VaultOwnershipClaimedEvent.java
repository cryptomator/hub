package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_ownership_claim")
@DiscriminatorValue(VaultOwnershipClaimedEvent.TYPE)
public class VaultOwnershipClaimedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_OWNERSHIP_CLAIM";

	@Column(name = "claimed_by")
	private @Nullable String claimedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	public @Nullable String getClaimedBy() {
		return claimedBy;
	}

	public void setClaimedBy(@Nullable String claimedBy) {
		this.claimedBy = claimedBy;
	}

	public @Nullable UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(@Nullable UUID vaultId) {
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
		return Objects.hash(super.getId(), claimedBy, vaultId);
	}

}
