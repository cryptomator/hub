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
@Table(name = "unlock_event")
@DiscriminatorValue(UnlockEvent.TYPE)
public class UnlockEvent extends AuditEvent {

	public static final String TYPE = "UNLOCK";

	@Column(name = "user_id")
	public String userId;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "device_id")
	public String deviceId;

	@Column(name = "result")
	@Enumerated(EnumType.STRING)
	public Result result;

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

	public static void log(String userId, UUID vaultId, String deviceId, Result result) {
		var event = new UnlockEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.deviceId = deviceId;
		event.result = result;
		event.persist();
	}

	public enum Result {
		SUCCESS,
		NO_SUCH_DEVICE,
		DEVICE_NOT_AUTHORIZED
	}

}
