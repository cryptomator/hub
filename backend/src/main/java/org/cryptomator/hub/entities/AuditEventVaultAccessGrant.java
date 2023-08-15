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
@Table(name = "audit_event_vault_access_grant")
@DiscriminatorValue(AuditEventVaultAccessGrant.TYPE)
@RegisterForReflection(targets = {UUID[].class})
public class AuditEventVaultAccessGrant extends AuditEvent {

	public static final String TYPE = "VAULT_ACCESS_GRANT";

	@Column(name = "granted_by")
	public String grantedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "authority_id")
	public String authorityId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEventVaultAccessGrant that = (AuditEventVaultAccessGrant) o;
		return super.equals(that) //
				&& Objects.equals(grantedBy, that.grantedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, grantedBy, vaultId, authorityId);
	}

	public static void log(String grantedBy, UUID vaultId, String authorityId) {
		var event = new AuditEventVaultAccessGrant();
		event.timestamp = Instant.now();
		event.grantedBy = grantedBy;
		event.vaultId = vaultId;
		event.authorityId = authorityId;
		event.persist();
	}

}
