package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class DeviceRemovedEventRepository implements PanacheRepository<DeviceRemovedEvent> {

	public void log(String removedBy, String deviceId) {
		var event = new DeviceRemovedEvent();
		event.setTimestamp(Instant.now());
		event.setRemovedBy(removedBy);
		event.setDeviceId(deviceId);
		persist(event);
	}

}
