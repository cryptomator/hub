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
@Table(name = "audit_event_vault_member_add")
@DiscriminatorValue(VaultMemberAddedEvent.TYPE)
public class VaultMemberAddedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_ADD";

	@Column(name = "added_by")
	private @Nullable String addedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "authority_id")
	private @Nullable String authorityId;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private VaultAccess.Role role;

	public @Nullable String getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(@Nullable String addedBy) {
		this.addedBy = addedBy;
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
		VaultMemberAddedEvent that = (VaultMemberAddedEvent) o;
		return super.equals(that) //
				&& Objects.equals(addedBy, that.addedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId) //
				&& Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), addedBy, vaultId, authorityId, role);
	}

}
