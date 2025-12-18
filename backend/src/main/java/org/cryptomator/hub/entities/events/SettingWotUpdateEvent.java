package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_event_setting_wot_update")
@DiscriminatorValue(SettingWotUpdateEvent.TYPE)
public class SettingWotUpdateEvent extends AuditEvent {

	public static final String TYPE = "SETTING_WOT_UPDATE";

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "wot_max_depth")
	private int wotMaxDepth;

	@Column(name = "wot_id_verify_len")
	private int wotIdVerifyLen;

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public int getWotMaxDepth() {
		return wotMaxDepth;
	}

	public void setWotMaxDepth(int wotMaxDepth) {
		this.wotMaxDepth = wotMaxDepth;
	}

	public int getWotIdVerifyLen() {
		return wotIdVerifyLen;
	}

	public void setWotIdVerifyLen(int wotIdVerifyLen) {
		this.wotIdVerifyLen = wotIdVerifyLen;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SettingWotUpdateEvent that = (SettingWotUpdateEvent) o;
		return wotMaxDepth == that.wotMaxDepth && wotIdVerifyLen == that.wotIdVerifyLen && Objects.equals(updatedBy, that.updatedBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), updatedBy, wotMaxDepth, wotIdVerifyLen);
	}
}
