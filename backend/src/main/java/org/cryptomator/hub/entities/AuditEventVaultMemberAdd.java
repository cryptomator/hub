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
@Table(name = "audit_event_vault_member_add")
@DiscriminatorValue(AuditEventVaultMemberAdd.TYPE)
public class AuditEventVaultMemberAdd extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_ADD";

	@Column(name = "added_by")
	public String addedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "authority_id")
	public String authorityId;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	public VaultAccess.Role role;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEventVaultMemberAdd that = (AuditEventVaultMemberAdd) o;
		return super.equals(that) //
				&& Objects.equals(addedBy, that.addedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId) //
				&& Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, addedBy, vaultId, authorityId, role);
	}

	public static void log(String addedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
		var event = new AuditEventVaultMemberAdd();
		event.timestamp = Instant.now();
		event.addedBy = addedBy;
		event.vaultId = vaultId;
		event.authorityId = authorityId;
		event.role = role;
		event.persist();
	}

}
