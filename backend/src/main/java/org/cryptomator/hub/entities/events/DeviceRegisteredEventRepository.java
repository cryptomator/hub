package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.cryptomator.hub.entities.Device;

import java.time.Instant;

@ApplicationScoped
public class DeviceRegisteredEventRepository implements PanacheRepository<DeviceRegisteredEvent> {

	public void log(String registeredBy, String deviceId, String deviceName, Device.Type deviceType) {
		var event = new DeviceRegisteredEvent();
		event.setTimestamp(Instant.now());
		event.setRegisteredBy(registeredBy);
		event.setDeviceId(deviceId);
		event.setDeviceName(deviceName);
		event.setDeviceType(deviceType);
		persist(event);
	}
}
