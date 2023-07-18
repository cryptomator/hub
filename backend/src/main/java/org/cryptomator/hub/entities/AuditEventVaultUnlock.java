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
@Table(name = "audit_event_vault_unlock")
@DiscriminatorValue(AuditEventVaultUnlock.TYPE)
public class AuditEventVaultUnlock extends AuditEvent {

	public static final String TYPE = "VAULT_UNLOCK";

	@Column(name = "unlocked_by")
	public String unlockedBy;

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
		AuditEventVaultUnlock that = (AuditEventVaultUnlock) o;
		return super.equals(that) //
				&& Objects.equals(unlockedBy, that.unlockedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(deviceId, that.deviceId) //
				&& Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, unlockedBy, vaultId, deviceId, result);
	}

	public static void log(String unlockedBy, UUID vaultId, String deviceId, Result result) {
		var event = new AuditEventVaultUnlock();
		event.timestamp = Instant.now();
		event.unlockedBy = unlockedBy;
		event.vaultId = vaultId;
		event.deviceId = deviceId;
		event.result = result;
		event.persist();
	}

	public enum Result {
		SUCCESS,
		UNAUTHORIZED
	}

}
