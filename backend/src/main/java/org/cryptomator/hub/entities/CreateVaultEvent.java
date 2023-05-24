package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "createvault_event")
@DiscriminatorValue(CreateVaultEvent.TYPE)
public class CreateVaultEvent extends AuditEvent {

	public static final String TYPE = "CREATE_VAULT";

	@Column(name = "user_id")
	public String userId;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CreateVaultEvent that = (CreateVaultEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId);
	}

	public static void log(String userId, UUID vaultId) {
		var event = new CreateVaultEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.persist();
	}
}
