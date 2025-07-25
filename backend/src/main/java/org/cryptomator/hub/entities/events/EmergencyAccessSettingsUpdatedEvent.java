package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "audit_event_emergaccess_settings_updated")
@DiscriminatorValue(EmergencyAccessSettingsUpdatedEvent.TYPE)
public class EmergencyAccessSettingsUpdatedEvent extends AuditEvent {

	public static final String TYPE = "EMERGENCY_ACCESS_SETTINGS_UPDATED";

	@Column(name = "admin_id", nullable = false)
	private String adminId;

	@Column(name = "council_member_ids", nullable = false)
	private String councilMemberIds;

	@Column(name = "required_key_shares", nullable = false)
	private int requiredKeyShares;

	@Column(name = "allow_choosing_council", nullable = false)
	private boolean allowChoosingCouncil;

	public String getAdminId() {
		return adminId;
	}

	public void setAdminId(String adminId) {
		this.adminId = adminId;
	}

	public String getCouncilMemberIds() {
		return councilMemberIds;
	}

	public void setCouncilMemberIds(String councilMemberIds) {
		this.councilMemberIds = councilMemberIds;
	}

	public int getRequiredKeyShares() {
		return requiredKeyShares;
	}

	public void setRequiredKeyShares(int requiredKeyShares) {
		this.requiredKeyShares = requiredKeyShares;
	}

	public boolean isAllowChoosingCouncil() {
		return allowChoosingCouncil;
	}

	public void setAllowChoosingCouncil(boolean allowChoosingCouncil) {
		this.allowChoosingCouncil = allowChoosingCouncil;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		EmergencyAccessSettingsUpdatedEvent other = (EmergencyAccessSettingsUpdatedEvent) o;
		return requiredKeyShares == other.requiredKeyShares
				&& allowChoosingCouncil == other.allowChoosingCouncil
				&& Objects.equals(adminId, other.adminId)
				&& Objects.equals(councilMemberIds, other.councilMemberIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), adminId, councilMemberIds, requiredKeyShares, allowChoosingCouncil);
	}
}

