package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_member_remove")
@DiscriminatorValue(VaultMemberRemovedEvent.TYPE)
public class VaultMemberRemovedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_REMOVE";

	@Column(name = "removed_by")
	private String removedBy;

	@Column(name = "vault_id")
	private UUID vaultId;

	@Column(name = "authority_id")
	private String authorityId;

	public String getRemovedBy() {
		return removedBy;
	}

	public void setRemovedBy(String removedBy) {
		this.removedBy = removedBy;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultMemberRemovedEvent that = (VaultMemberRemovedEvent) o;
		return super.equals(that) //
				&& Objects.equals(removedBy, that.removedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), removedBy, vaultId, authorityId);
	}

}
