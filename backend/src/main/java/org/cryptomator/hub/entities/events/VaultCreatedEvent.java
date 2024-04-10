package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_create")
@DiscriminatorValue(VaultCreatedEvent.TYPE)
public class VaultCreatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_CREATE";

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "vault_id")
	private UUID vaultId;

	@Column(name = "vault_name")
	private String vaultName;

	@Column(name = "vault_description")
	private String vaultDescription;

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultCreatedEvent that = (VaultCreatedEvent) o;
		return super.equals(that) //
				&& Objects.equals(createdBy, that.createdBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(vaultName, that.vaultName) //
				&& Objects.equals(vaultDescription, that.vaultDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), createdBy, vaultId, vaultName, vaultDescription);
	}

}
