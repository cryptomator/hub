package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_event_device_remove")
@DiscriminatorValue(AuditEventDeviceRemove.TYPE)
public class AuditEventDeviceRemove extends AuditEvent {

	public static final String TYPE = "DEVICE_REMOVE";

	@Column(name = "removed_by")
	public String removedBy;

	@Column(name = "device_id")
	public String deviceId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEventDeviceRemove that = (AuditEventDeviceRemove) o;
		return super.equals(that) //
				&& Objects.equals(removedBy, that.removedBy) //
				&& Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, removedBy, deviceId);
	}

	public static void log(String removedBy, String deviceId) {
		var event = new AuditEventDeviceRemove();
		event.timestamp = Instant.now();
		event.removedBy = removedBy;
		event.deviceId = deviceId;
		event.persist();
	}

}
