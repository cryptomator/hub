package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class VaultAccessRepository implements PanacheRepositoryBase<VaultAccess, VaultAccess.Id> {

	public Stream<VaultAccess> forVault(UUID vaultId) {
		return find("#VaultAccess.forVault", Parameters.with("vaultId", vaultId)).stream();
	}

}
