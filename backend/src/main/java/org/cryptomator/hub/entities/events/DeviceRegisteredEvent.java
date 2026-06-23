package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.cryptomator.hub.entities.Device.Type;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "audit_event_device_register")
@DiscriminatorValue(DeviceRegisteredEvent.TYPE)
public class DeviceRegisteredEvent extends AuditEvent {

	public static final String TYPE = "DEVICE_REGISTER";

	@Column(name = "registered_by")
	private @Nullable String registeredBy;

	@Column(name = "device_id")
	private @Nullable String deviceId;

	@Column(name = "device_name")
	private @Nullable String deviceName;

	@Column(name = "device_type")
	@Enumerated(EnumType.STRING)
	private @Nullable Type deviceType;

	public @Nullable String getRegisteredBy() {
		return registeredBy;
	}

	public void setRegisteredBy(@Nullable String registeredBy) {
		this.registeredBy = registeredBy;
	}

	public @Nullable String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(@Nullable String deviceId) {
		this.deviceId = deviceId;
	}

	public @Nullable String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(@Nullable String deviceName) {
		this.deviceName = deviceName;
	}

	public @Nullable Type getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(@Nullable Type deviceType) {
		this.deviceType = deviceType;
	}

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
		return Objects.hash(super.getId(), registeredBy, deviceId, deviceName, deviceType);
	}
}
