package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_update")
@DiscriminatorValue(VaultUpdatedEvent.TYPE)
public class VaultUpdatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_UPDATE";

	@Column(name = "updated_by")
	public String updatedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "vault_name")
	public String vaultName;

	@Column(name = "vault_description")
	public String vaultDescription;

	@Column(name = "vault_archived")
	public boolean vaultArchived;

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
		return Objects.hash(id, updatedBy, vaultId, vaultName, vaultDescription, vaultArchived);
	}

	public static void log(String updatedBy, UUID vaultId, String vaultName, String vaultDescription, boolean vaultArchived) {
		var event = new VaultUpdatedEvent();
		event.timestamp = Instant.now();
		event.updatedBy = updatedBy;
		event.vaultId = vaultId;
		event.vaultName = vaultName;
		event.vaultDescription = vaultDescription;
		event.vaultArchived = vaultArchived;
		event.persist();
	}

}
