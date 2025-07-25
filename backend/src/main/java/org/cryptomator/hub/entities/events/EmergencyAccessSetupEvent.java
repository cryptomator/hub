package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_emergaccess_setup")
@DiscriminatorValue(EmergencyAccessSetupEvent.TYPE)
public class EmergencyAccessSetupEvent extends AuditEvent {

	public static final String TYPE = "EMERGENCY_ACCESS_SETUP";

	@Column(name = "vault_id", nullable = false)
	private UUID vaultId;

	@Column(name = "owner_id", nullable = false)
	private String ownerId;

	@Column(name = "settings", nullable = false)
	private String settings;

	@Column(name = "ip_address", nullable = false)
	private String ipAddress;

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		EmergencyAccessSetupEvent other = (EmergencyAccessSetupEvent) o;
		return Objects.equals(vaultId, other.vaultId)
				&& Objects.equals(ownerId, other.ownerId)
				&& Objects.equals(settings, other.settings)
				&& Objects.equals(ipAddress, other.ipAddress);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), vaultId, ownerId, settings, ipAddress);
	}
}
