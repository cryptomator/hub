package org.cryptomator.hub.entities.events;

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
@Table(name = "audit_event_vault_key_retrieve")
@DiscriminatorValue(VaultKeyRetrievedEvent.TYPE)
//TODO: bad naming
public class VaultKeyRetrievedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_KEY_RETRIEVE";

	@Column(name = "retrieved_by")
	public String retrievedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "result")
	@Enumerated(EnumType.STRING)
	public Result result;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultKeyRetrievedEvent that = (VaultKeyRetrievedEvent) o;
		return super.equals(that) //
				&& Objects.equals(retrievedBy, that.retrievedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, retrievedBy, vaultId, result);
	}

	public static void log(String retrievedBy, UUID vaultId, Result result) {
		var event = new VaultKeyRetrievedEvent();
		event.timestamp = Instant.now();
		event.retrievedBy = retrievedBy;
		event.vaultId = vaultId;
		event.result = result;
		event.persist();
	}

	public enum Result {
		SUCCESS,
		UNAUTHORIZED
	}

}
