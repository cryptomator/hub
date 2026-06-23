package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_create")
@DiscriminatorValue(VaultCreatedEvent.TYPE)
public class VaultCreatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_CREATE";

	@Column(name = "created_by")
	private @Nullable String createdBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "vault_name")
	private @Nullable String vaultName;

	@Column(name = "vault_description")
	private @Nullable String vaultDescription;

	public @Nullable String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(@Nullable String createdBy) {
		this.createdBy = createdBy;
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
