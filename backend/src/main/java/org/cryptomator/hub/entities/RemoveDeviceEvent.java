package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "remove_device_event")
@DiscriminatorValue(RemoveDeviceEvent.TYPE)
public class RemoveDeviceEvent extends AuditEvent {

	public static final String TYPE = "REMOVE_DEVICE";

	@Column(name = "removed_by")
	public String removedBy;

	@Column(name = "device_id")
	public String deviceId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RemoveDeviceEvent that = (RemoveDeviceEvent) o;
		return super.equals(that) //
				&& Objects.equals(removedBy, that.removedBy) //
				&& Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, removedBy, deviceId);
	}

	public static void log(String removedBy, String deviceId) {
		var event = new RemoveDeviceEvent();
		event.timestamp = Instant.now();
		event.removedBy = removedBy;
		event.deviceId = deviceId;
		event.persist();
	}

}
