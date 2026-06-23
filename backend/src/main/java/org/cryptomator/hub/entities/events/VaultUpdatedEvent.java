package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_update")
@DiscriminatorValue(VaultUpdatedEvent.TYPE)
public class VaultUpdatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_UPDATE";

	@Column(name = "updated_by")
	private @Nullable String updatedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "vault_name")
	private @Nullable String vaultName;

	@Column(name = "vault_description")
	private @Nullable String vaultDescription;

	@Column(name = "vault_archived")
	private boolean vaultArchived;

	public @Nullable String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(@Nullable String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public @Nullable UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(@Nullable UUID vaultId) {
		this.vaultId = vaultId;
	}

	public @Nullable String getVaultName() {
		return vaultName;
	}

	public void setVaultName(@Nullable String vaultName) {
		this.vaultName = vaultName;
	}

	public @Nullable String getVaultDescription() {
		return vaultDescription;
	}

	public void setVaultDescription(@Nullable String vaultDescription) {
		this.vaultDescription = vaultDescription;
	}

	public boolean isVaultArchived() {
		return vaultArchived;
	}

	public void setVaultArchived(boolean vaultArchived) {
		this.vaultArchived = vaultArchived;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultUpdatedEvent that = (VaultUpdatedEvent) o;
		return super.equals(that) //
				&& Objects.equals(updatedBy, that.updatedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(vaultName, that.vaultName) //
				&& Objects.equals(vaultDescription, that.vaultDescription) //
				&& Objects.equals(vaultArchived, that.vaultArchived);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), updatedBy, vaultId, vaultName, vaultDescription, vaultArchived);
	}

}
