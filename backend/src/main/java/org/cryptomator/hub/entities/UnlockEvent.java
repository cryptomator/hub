package org.cryptomator.hub.entities;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "unlock_event")
@DiscriminatorValue("UNLOCK")
public final class UnlockEvent extends AuditEvent {

	@Column(name = "user_id")
	public String userId;

	@Column(name = "vault_id")
	public String vaultId;

	@Column(name = "device_id")
	public String deviceId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UnlockEvent that = (UnlockEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId, deviceId);
	}

	public static void log(User user, Vault vault, Device device) {
		var event = new UnlockEvent();
		event.id = UUID.randomUUID().toString();
		event.timestamp = Timestamp.from(Instant.now());
		event.message = "%s unlocked %s using device %s".formatted(user.email, vault.name, device.name);
		event.userId = user.id;
		event.vaultId = vault.id;
		event.deviceId = device.id;
		event.persist();
	}

}
