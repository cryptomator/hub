package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.UUID;

@ApplicationScoped
public class AccessTokenRepository implements PanacheRepositoryBase<AccessToken, AccessToken.AccessId> {

	public AccessToken unlock(UUID vaultId, String userId) {
		try {
			return find("#AccessToken.get", Parameters.with("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public void deleteByUser(String userId) {
		delete("#AccessToken.deleteByUser", Parameters.with("userId", userId));
	}

}
