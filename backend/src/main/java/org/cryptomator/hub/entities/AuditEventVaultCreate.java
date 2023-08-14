package org.cryptomator.hub.entities;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_create")
@DiscriminatorValue(AuditEventVaultCreate.TYPE)
@RegisterForReflection(targets = {UUID[].class})
public class AuditEventVaultCreate extends AuditEvent {

	public static final String TYPE = "VAULT_CREATE";

	@Column(name = "created_by")
	public String createdBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "vault_name")
	public String vaultName;

	@Column(name = "vault_description")
	public String vaultDescription;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEventVaultCreate that = (AuditEventVaultCreate) o;
		return super.equals(that) //
				&& Objects.equals(createdBy, that.createdBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(vaultName, that.vaultName) //
				&& Objects.equals(vaultDescription, that.vaultDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, createdBy, vaultId, vaultName, vaultDescription);
	}

	public static void log(String createdBy, UUID vaultId, String vaultName, String vaultDescription) {
		var event = new AuditEventVaultCreate();
		event.timestamp = Instant.now();
		event.createdBy = createdBy;
		event.vaultId = vaultId;
		event.vaultName = vaultName;
		event.vaultDescription = vaultDescription;
		event.persist();
	}

}
