package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_member_remove")
@DiscriminatorValue(VaultMemberRemovedEvent.TYPE)
public class VaultMemberRemovedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_MEMBER_REMOVE";

	@Column(name = "removed_by")
	public String removedBy;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "authority_id")
	public String authorityId;

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
		return Objects.hash(id, removedBy, vaultId, authorityId);
	}

	public static void log(String removedBy, UUID vaultId, String authorityId) {
		var event = new VaultMemberRemovedEvent();
		event.timestamp = Instant.now();
		event.removedBy = removedBy;
		event.vaultId = vaultId;
		event.authorityId = authorityId;
		event.persist();
	}

}
