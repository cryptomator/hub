package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_key_retrieve")
@DiscriminatorValue(VaultKeyRetrievedEvent.TYPE)
public class VaultKeyRetrievedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_KEY_RETRIEVE";

	@Column(name = "retrieved_by")
	private @Nullable String retrievedBy;

	@Column(name = "vault_id")
	private @Nullable UUID vaultId;

	@Column(name = "result")
	@Enumerated(EnumType.STRING)
	private @Nullable Result result;

	@Column(name = "ip_address")
	private @Nullable String ipAddress;

	@Column(name = "device_id")
	private @Nullable String deviceId;

	public @Nullable String getRetrievedBy() {
		return retrievedBy;
	}

	public void setRetrievedBy(@Nullable String retrievedBy) {
		this.retrievedBy = retrievedBy;
	}

	public @Nullable UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(@Nullable UUID vaultId) {
		this.vaultId = vaultId;
	}

	public @Nullable Result getResult() {
		return result;
	}

	public void setResult(@Nullable Result result) {
		this.result = result;
	}

	public @Nullable String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(@Nullable String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public @Nullable String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(@Nullable String device) {
		this.deviceId = device;
	}

	public enum Result {
		SUCCESS,
		UNAUTHORIZED
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		VaultKeyRetrievedEvent that = (VaultKeyRetrievedEvent) o;
		return Objects.equals(retrievedBy, that.retrievedBy) && Objects.equals(vaultId, that.vaultId) && result == that.result && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), retrievedBy, vaultId, result, ipAddress, deviceId);
	}
}
