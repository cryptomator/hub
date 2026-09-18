package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "audit_event_setting_auto_grant_update")
@DiscriminatorValue(SettingAutoGrantUpdateEvent.TYPE)
public class SettingAutoGrantUpdateEvent extends AuditEvent {

	public static final String TYPE = "SETTING_AUTO_GRANT_UPDATE";

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "enabled")
	private boolean enabled;

	@Column(name = "trust_threshold")
	private int trustThreshold;

	@Column(name = "allow_override")
	private boolean allowOverride;

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getTrustThreshold() {
		return trustThreshold;
	}

	public void setTrustThreshold(int trustThreshold) {
		this.trustThreshold = trustThreshold;
	}

	public boolean isAllowOverride() {
		return allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SettingAutoGrantUpdateEvent that = (SettingAutoGrantUpdateEvent) o;
		return enabled == that.enabled && trustThreshold == that.trustThreshold && allowOverride == that.allowOverride && Objects.equals(updatedBy, that.updatedBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), updatedBy, enabled, trustThreshold, allowOverride);
	}
}
