package org.cryptomator.hub;

import io.quarkus.scheduler.Scheduled;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
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
		syncAddedUsers(keycloakUsers, databaseUsers);
		var deletedUsers = syncDeletedUsers(keycloakUsers, databaseUsers);
		syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUsers);
		syncAddedGroups(keycloakGroups, databaseGroups);
		var deletedGroups = syncDeletedGroups(keycloakGroups, databaseGroups);
		syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroups);
	}

	//visible for testing
	void syncAddedUsers(Map<String, User> keycloakUsers, Map<String, User> databaseUsers) {
		var addedUsers = diff(keycloakUsers.keySet(), databaseUsers.keySet());
		for (var id : addedUsers) {
			keycloakUsers.get(id).persist();
		}
	}

	//visible for testing
	Set<String> syncDeletedUsers(Map<String, User> keycloakUsers, Map<String, User> databaseUsers) {
		var deletedUsers = diff(databaseUsers.keySet(), keycloakUsers.keySet());
		for (var id : deletedUsers) {
			databaseUsers.get(id).delete();
		}
		return deletedUsers;
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
	void syncAddedGroups(Map<String, Group> keycloakGroups, Map<String, Group> databaseGroups) {
		var addedGroups = diff(keycloakGroups.keySet(), databaseGroups.keySet());
		for (var id : addedGroups) {
			keycloakGroups.get(id).persist();
		}
	}

	//visible for testing
	Set<String> syncDeletedGroups(Map<String, Group> keycloakGroups, Map<String, Group> databaseGroups) {
		var deletedGroups = diff(databaseGroups.keySet(), keycloakGroups.keySet());
		for (var id : deletedGroups) {
			databaseGroups.get(id).delete();
		}
		return deletedGroups;
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
		}
	}

	private <T> Set<T> diff(Set<T> base, Set<T> difference) {
		var result = new HashSet<>(base);
		result.removeAll(difference);
		return result;
	}

}