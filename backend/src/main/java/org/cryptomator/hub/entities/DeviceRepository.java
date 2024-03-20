package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class DeviceRepository implements PanacheRepositoryBase<Device, String> {

	public Device findByIdAndUser(String deviceId, String userId) throws NoResultException {
		return find("#Device.findByIdAndOwner", Parameters.with("deviceId", deviceId).and("userId", userId)).singleResult();
	}

	public Stream<Device> findAllInList(List<String> ids) {
		return find("#Device.allInList", Parameters.with("ids", ids)).stream();
	}

	public void deleteByOwner(String userId) {
		delete("#Device.deleteByOwner", Parameters.with("userId", userId));
	}


}
