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
@Table(name = "update_vault_membership_event")
@DiscriminatorValue(UpdateVaultMembershipEvent.TYPE)
public class UpdateVaultMembershipEvent extends AuditEvent {

	public static final String TYPE = "UPDATE_VAULT_MEMBERSHIP";

	@Column(name = "user_id")
	public String userId;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "authority_id")
	public String authorityId;

	@Column(name = "operation")
	@Enumerated(EnumType.STRING)
	public Operation operation;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UpdateVaultMembershipEvent that = (UpdateVaultMembershipEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(authorityId, that.authorityId) //
				&& Objects.equals(operation, that.operation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId, authorityId, operation);
	}

	public static void log(String userId, UUID vaultId, String authorityId, Operation operation) {
		var event = new UpdateVaultMembershipEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.authorityId = authorityId;
		event.operation = operation;
		event.persist();
	}

	public enum Operation {
		ADD,
		REMOVE
	}

}
