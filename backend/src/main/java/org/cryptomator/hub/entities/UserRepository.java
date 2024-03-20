package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, String> {

	public Stream<User> findRequiringAccessGrant(UUID vaultId) {
		return find("#User.requiringAccessGrant", Parameters.with("vaultId", vaultId)).stream();
	}

	public long countEffectiveGroupUsers(String groupdId) {
		return count("#User.countEffectiveGroupUsers", Parameters.with("groupId", groupdId));
	}

	public Stream<User> getEffectiveGroupUsers(String groupdId) {
		return find("#User.getEffectiveGroupUsers", Parameters.with("groupId", groupdId)).stream();
	}
}
