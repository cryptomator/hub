package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "audit_event_device_remove")
@DiscriminatorValue(DeviceRemovedEvent.TYPE)
public class DeviceRemovedEvent extends AuditEvent {

	public static final String TYPE = "DEVICE_REMOVE";

	@Column(name = "removed_by")
	private @Nullable String removedBy;

	@Column(name = "device_id")
	private @Nullable String deviceId;

	public @Nullable String getRemovedBy() {
		return removedBy;
	}

	public void setRemovedBy(@Nullable String removedBy) {
		this.removedBy = removedBy;
	}

	public @Nullable String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(@Nullable String deviceId) {
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
		return Objects.hash(super.getId(), removedBy, deviceId);
	}

}
