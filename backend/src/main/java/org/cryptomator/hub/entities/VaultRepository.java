package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class VaultRepository implements PanacheRepositoryBase<Vault, UUID> {

	public Stream<Vault> findAccessibleByUser(String userId) {
		return find("#Vault.accessibleByUser", Parameters.with("userId", userId)).stream();
	}

	public Stream<Vault> findAccessibleByUser(String userId, VaultAccess.Role role) {
		return find("#Vault.accessibleByUserAndRole", Parameters.with("userId", userId).and("role", role)).stream();
	}

	public Stream<Vault> findAllInList(List<UUID> ids) {
		return find("#Vault.allInList", Parameters.with("ids", ids)).stream();
	}

}
