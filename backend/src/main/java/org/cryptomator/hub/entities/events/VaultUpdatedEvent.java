package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_update")
@DiscriminatorValue(VaultUpdatedEvent.TYPE)
public class VaultUpdatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_UPDATE";

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "vault_id")
	private UUID vaultId;

	@Column(name = "vault_name")
	private String vaultName;

	@Column(name = "vault_description")
	private String vaultDescription;

	@Column(name = "vault_archived")
	private boolean vaultArchived;

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public String getVaultName() {
		return vaultName;
	}

	public void setVaultName(String vaultName) {
		this.vaultName = vaultName;
	}

	public String getVaultDescription() {
		return vaultDescription;
	}

	public void setVaultDescription(String vaultDescription) {
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
