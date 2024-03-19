package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.cryptomator.hub.entities.Device;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_event_device_register")
@DiscriminatorValue(DeviceRegisteredEvent.TYPE)
public class DeviceRegisteredEvent extends AuditEvent {

	public static final String TYPE = "DEVICE_REGISTER";

	@Column(name = "registered_by")
	String registeredBy;

	@Column(name = "device_id")
	String deviceId;

	@Column(name = "device_name")
	String deviceName;

	@Column(name = "device_type")
	@Enumerated(EnumType.STRING)
	Device.Type deviceType;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DeviceRegisteredEvent that = (DeviceRegisteredEvent) o;
		return super.equals(that) //
				&& Objects.equals(registeredBy, that.registeredBy) //
				&& Objects.equals(deviceId, that.deviceId) //
				&& Objects.equals(deviceName, that.deviceName) //
				&& Objects.equals(deviceType, that.deviceType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, registeredBy, deviceId, deviceName, deviceType);
	}

	public static void log(String registeredBy, String deviceId, String deviceName, Device.Type deviceType) {
		var event = new DeviceRegisteredEvent();
		event.timestamp = Instant.now();
		event.registeredBy = registeredBy;
		event.deviceId = deviceId;
		event.deviceName = deviceName;
		event.deviceType = deviceType;
		event.persist();
	}

}
