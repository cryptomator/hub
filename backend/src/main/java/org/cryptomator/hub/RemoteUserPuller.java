package org.cryptomator.hub;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class RemoteUserPuller {

	@Inject
	RemoteUserProvider remoteUserProvider;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	void sync() {
		var keycloakGroups = remoteUserProvider.groups().stream().collect(Collectors.toMap(g -> g.id, Function.identity()));
		var keycloakUsers = remoteUserProvider.users().stream().collect(Collectors.toMap(u -> u.id, Function.identity()));
		sync(keycloakGroups, keycloakUsers);
	}

	@Transactional
	void sync(Map<String, Group> keycloakGroups, Map<String, User> keycloakUsers) {
		var databaseGroups = Group.<Group>findAll().stream().collect(Collectors.toMap(g -> g.id, Function.identity()));
		var databaseUsers = User.<User>findAll().stream().collect(Collectors.toMap(u -> u.id, Function.identity()));
		sync(keycloakGroups, keycloakUsers, databaseGroups, databaseUsers);
	}

	//visible for testing
	void sync(Map<String, Group> keycloakGroups, Map<String, User> keycloakUsers, Map<String, Group> databaseGroups, Map<String, User> databaseUsers) {
		syncAddedAuthorities(keycloakUsers, databaseUsers);
		var deletedUsers = syncDeletedAuthorities(keycloakUsers, databaseUsers);
		syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUsers);
		syncAddedAuthorities(keycloakGroups, databaseGroups);
		var deletedGroups = syncDeletedAuthorities(keycloakGroups, databaseGroups);
		syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroups);
	}

	//visible for testing
	<T extends Authority> void syncAddedAuthorities(Map<String, T> keycloakAuthorities, Map<String, T> databaseAuthorities) {
		var addedAuthority = diff(keycloakAuthorities.keySet(), databaseAuthorities.keySet());
		for (var id : addedAuthority) {
			keycloakAuthorities.get(id).persist();
		}
	}

	//visible for testing
	<T extends Authority> Set<String> syncDeletedAuthorities(Map<String, T> keycloakAuthorities, Map<String, T> databaseAuthorities) {
		var deletedAuthorities = diff(databaseAuthorities.keySet(), keycloakAuthorities.keySet());
		for (var id : deletedAuthorities) {
			databaseAuthorities.get(id).delete();
		}
		return deletedAuthorities;
	}

	//visible for testing
	void syncUpdatedUsers(Map<String, User> keycloakUsers, Map<String, User> databaseUsers, Set<String> deletedUsers) {
		var updatedUsers = diff(databaseUsers.keySet(), deletedUsers);
		for (var id : updatedUsers) {
			var dbUser = databaseUsers.get(id);
			var kcUser = keycloakUsers.get(id);
			dbUser.pictureUrl = kcUser.pictureUrl;
			dbUser.name = kcUser.name;
			dbUser.email = kcUser.email;
			dbUser.persist();
		}
	}

	//visible for testing
	void syncUpdatedGroups(Map<String, Group> keycloakGroups, Map<String, Group> databaseGroups, Set<String> deletedGroups) {
		var updatedGroups = diff(databaseGroups.keySet(), deletedGroups);
		for (var id : updatedGroups) {
			var dbGroup = databaseGroups.get(id);
			var kcGroup = keycloakGroups.get(id);

			dbGroup.name = kcGroup.name;

			dbGroup.members.addAll(diff(kcGroup.members, dbGroup.members));
			dbGroup.members.removeAll(diff(dbGroup.members, kcGroup.members));
			// TODO why don't we run dbGroup.persist()?
		}
	}

	private <T> Set<T> diff(Set<T> base, Set<T> difference) {
		var result = new HashSet<>(base);
		result.removeAll(difference);
		return result;
	}

}