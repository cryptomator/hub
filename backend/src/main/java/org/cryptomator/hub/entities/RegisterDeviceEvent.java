package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "register_device_event")
@DiscriminatorValue(RegisterDeviceEvent.TYPE)
public class RegisterDeviceEvent extends AuditEvent {

	public static final String TYPE = "REGISTER_DEVICE";

	@Column(name = "user_id")
	public String userId;

	@Column(name = "device_id")
	public String deviceId;

	@Column(name = "device_name")
	public String deviceName;

	@Column(name = "device_type")
	@Enumerated(EnumType.STRING)
	public Device.Type deviceType;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RegisterDeviceEvent that = (RegisterDeviceEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(deviceId, that.deviceId) //
				&& Objects.equals(deviceName, that.deviceName) //
				&& Objects.equals(deviceType, that.deviceType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, deviceId, deviceName, deviceType);
	}

	public static void log(String userId, String deviceId, String deviceName, Device.Type deviceType) {
		var event = new RegisterDeviceEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.deviceId = deviceId;
		event.deviceName = deviceName;
		event.deviceType = deviceType;
		event.persist();
	}

}