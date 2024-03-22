package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
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
@Table(name = "audit_event_vault_member_add")
@DiscriminatorValue(VaultMemberAddedEvent.TYPE)
public class VaultMemberAddedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_ADD";

	@Column(name = "added_by")
	String addedBy;

	@Column(name = "vault_id")
	UUID vaultId;

	@Column(name = "authority_id")
	String authorityId;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	VaultAccess.Role role;

	public String getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(String addedBy) {
		this.addedBy = addedBy;
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
		VaultMemberAddedEvent that = (VaultMemberAddedEvent) o;
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

	@ApplicationScoped
	public static class Repository implements PanacheRepository<VaultMemberAddedEvent> {

		public void log(String addedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
			var event = new VaultMemberAddedEvent();
			event.setTimestamp(Instant.now());
			event.setAddedBy(addedBy);
			event.setVaultId(vaultId);
			event.setAuthorityId(authorityId);
			event.setRole(role);
			persist(event);
		}
	}
}
