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
@Table(name = "unlockvault_event")
@DiscriminatorValue(UnlockVaultEvent.TYPE)
public class UnlockVaultEvent extends AuditEvent {

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
		UnlockVaultEvent that = (UnlockVaultEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(deviceId, that.deviceId) //
				&& Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId, deviceId, result);
	}

	public static void log(String userId, UUID vaultId, String deviceId, Result result) {
		var event = new UnlockVaultEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.deviceId = deviceId;
		event.result = result;
		event.persist();
	}

	public enum Result {
		SUCCESS,
		UNAUTHORIZED;
	}

}
