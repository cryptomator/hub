package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.cryptomator.hub.entities.VaultAccess;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_member_update")
@DiscriminatorValue(VaultMemberUpdatedEvent.TYPE)
public class VaultMemberUpdatedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_UPDATE";

	@Column(name = "updated_by")
	private @Nullable String updatedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "authority_id")
	private @Nullable String authorityId;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private VaultAccess.Role role;

	public @Nullable String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(@Nullable String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public @Nullable UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(@Nullable UUID vaultId) {
		this.vaultId = vaultId;
	}

	public @Nullable String getAuthorityId() {
		return authorityId;
	}

	public void setAuthorityId(@Nullable String authorityId) {
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
		return Objects.hash(super.getId(), updatedBy, vaultId, authorityId, role);
	}

}
