package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_access_grant")
@DiscriminatorValue(VaultAccessGrantedEvent.TYPE)
public class VaultAccessGrantedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_ACCESS_GRANT";

	@Column(name = "granted_by")
	String grantedBy;

	@Column(name = "vault_id")
	UUID vaultId;

	@Column(name = "authority_id")
	String authorityId;

	public String getGrantedBy() {
		return grantedBy;
	}

	public void setGrantedBy(String grantedBy) {
		this.grantedBy = grantedBy;
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
		VaultAccessGrantedEvent that = (VaultAccessGrantedEvent) o;
		return super.equals(that) //
				&& Objects.equals(grantedBy, that.grantedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, grantedBy, vaultId, authorityId);
	}

}
