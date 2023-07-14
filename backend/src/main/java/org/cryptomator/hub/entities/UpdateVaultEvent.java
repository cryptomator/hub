package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "update_vault_event")
@DiscriminatorValue(UpdateVaultEvent.TYPE)
public class UpdateVaultEvent extends AuditEvent {

	public static final String TYPE = "UPDATE_VAULT";

	@Column(name = "user_id")
	public String userId;

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
		UpdateVaultEvent that = (UpdateVaultEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(vaultName, that.vaultName) //
				&& Objects.equals(vaultDescription, that.vaultDescription) //
				&& Objects.equals(vaultArchived, that.vaultArchived);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId, vaultName, vaultDescription, vaultArchived);
	}

	public static void log(String userId, UUID vaultId, String vaultName, String vaultDescription, boolean vaultArchived) {
		var event = new UpdateVaultEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.vaultName = vaultName;
		event.vaultDescription = vaultDescription;
		event.vaultArchived = vaultArchived;
		event.persist();
	}

}