package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class EffectiveVaultAccessRepository implements PanacheRepositoryBase<EffectiveVaultAccess, EffectiveVaultAccess.Id> {


	public boolean isUserOccupyingSeat(String userId) {
		return count("#EffectiveVaultAccess.countSeatsOccupiedBySingleUser", Parameters.with("userId", userId)) > 0;
	}

	public long countSeatsOccupiedByUsers(List<String> userIds) {
		return count("#EffectiveVaultAccess.countSeatsOccupiedByUsers", Parameters.with("userIds", userIds));
	}

	public long countSeatOccupyingUsers() {
		return count("#EffectiveVaultAccess.countSeatOccupyingUsers");
	}

	public long countSeatOccupyingUsersWithAccessToken() {
		return count("#EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken");
	}

	public long countSeatOccupyingUsersOfGroup(String groupId) {
		return count("#EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", Parameters.with("groupId", groupId));
	}

	public Collection<VaultAccess.Role> listRoles(UUID vaultId, String authorityId) {
		return find("#EffectiveVaultAccess.findByAuthorityAndVault", Parameters.with("vaultId", vaultId).and("authorityId", authorityId)).stream()
				.map(eva -> eva.getId().getRole())
				.collect(Collectors.toUnmodifiableSet());
	}
}
