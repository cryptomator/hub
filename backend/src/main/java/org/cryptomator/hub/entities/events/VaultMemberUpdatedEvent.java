package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.cryptomator.hub.entities.VaultAccess;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_member_update")
@DiscriminatorValue(VaultMemberUpdatedEvent.TYPE)
public class VaultMemberUpdatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_UPDATE";

	@Column(name = "updated_by")
	String updatedBy;

	@Column(name = "vault_id")
	UUID vaultId;

	@Column(name = "authority_id")
	String authorityId;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	VaultAccess.Role role;

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public String getAuthorityId() {
		return authorityId;
	}

	public void setAuthorityId(String authorityId) {
		this.authorityId = authorityId;
	}

	public VaultAccess.Role getRole() {
		return role;
	}

	public void setRole(VaultAccess.Role role) {
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultMemberUpdatedEvent that = (VaultMemberUpdatedEvent) o;
		return super.equals(that) //
				&& Objects.equals(updatedBy, that.updatedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId) //
				&& Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, updatedBy, vaultId, authorityId, role);
	}

}
