package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_event_device_remove")
@DiscriminatorValue(DeviceRemovedEvent.TYPE)
public class DeviceRemovedEvent extends AuditEvent {

	public static final String TYPE = "DEVICE_REMOVE";

	@Column(name = "removed_by")
	String removedBy;

	@Column(name = "device_id")
	String deviceId;

	public String getRemovedBy() {
		return removedBy;
	}

	public void setRemovedBy(String removedBy) {
		this.removedBy = removedBy;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DeviceRemovedEvent that = (DeviceRemovedEvent) o;
		return super.equals(that) //
				&& Objects.equals(removedBy, that.removedBy) //
				&& Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, removedBy, deviceId);
	}

}
