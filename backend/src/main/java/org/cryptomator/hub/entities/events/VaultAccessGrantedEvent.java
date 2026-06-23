package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_access_grant")
@DiscriminatorValue(VaultAccessGrantedEvent.TYPE)
public class VaultAccessGrantedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_ACCESS_GRANT";

	@Column(name = "granted_by")
	private @Nullable String grantedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "authority_id")
	private @Nullable String authorityId;

	public @Nullable String getGrantedBy() {
		return grantedBy;
	}

	public void setGrantedBy(@Nullable String grantedBy) {
		this.grantedBy = grantedBy;
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
		return Objects.hash(super.getId(), grantedBy, vaultId, authorityId);
	}

}
