package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
@Deprecated
public class LegacyAccessTokenRepository implements PanacheRepositoryBase<LegacyAccessToken, LegacyAccessToken.AccessId> {

	public LegacyAccessToken unlock(UUID vaultId, String deviceId, String userId) {
		return find("#LegacyAccessToken.get", Map.of("deviceId", deviceId, "vaultId", vaultId, "userId", userId))
				.firstResult();
	}

	public Stream<LegacyAccessToken> getByDeviceAndOwner(String deviceId, String userId) {
		return stream("#LegacyAccessToken.getByDevice", Map.of("deviceId", deviceId, "userId", userId));
	}

}
