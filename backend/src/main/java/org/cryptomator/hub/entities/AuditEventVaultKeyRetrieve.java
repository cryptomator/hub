package org.cryptomator.hub.entities;

import io.quarkus.runtime.annotations.RegisterForReflection;
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
@DiscriminatorValue(AuditEventVaultKeyRetrieve.TYPE)
@RegisterForReflection(targets = {UUID[].class})
public class AuditEventVaultKeyRetrieve extends AuditEvent {

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
		AuditEventVaultKeyRetrieve that = (AuditEventVaultKeyRetrieve) o;
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
		var event = new AuditEventVaultKeyRetrieve();
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
