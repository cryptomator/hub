package org.cryptomator.hub;

import io.quarkus.scheduler.Scheduled;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class RemoteUserPuller {

	@Inject
	SyncerConfig config;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	@Transactional
	void sync() {
		var remoteUserProvider = new RemoteUserProviderFactory().get(config);

		var keycloakGroups = remoteUserProvider.groupsIncludingMembers().collect(Collectors.toMap(g -> g.id, Function.identity()));
		var databaseGroups = Group.<Group>findAll().stream().collect(Collectors.toMap(g -> g.id, Function.identity()));
		var keycloakUsers = remoteUserProvider.users().collect(Collectors.toMap(u -> u.id, Function.identity()));
		var databaseUsers = User.<User>findAll().stream().collect(Collectors.toMap(u -> u.id, Function.identity()));

		var addedUsers = diff(keycloakUsers.keySet(), databaseUsers.keySet());
		for (var id : addedUsers) {
			keycloakUsers.get(id).persist();
		}

		var deletedUsers = diff(databaseUsers.keySet(), keycloakUsers.keySet());
		for (var id : deletedUsers) {
			databaseUsers.get(id).delete();
		}

		var updatedUsers = diff(databaseUsers.keySet(), deletedUsers);
		for (var id : updatedUsers) {
			var dbUser = databaseUsers.get(id);
			var kcUser = keycloakUsers.get(id);
			dbUser.pictureUrl = kcUser.pictureUrl;
			dbUser.name = kcUser.name;
			dbUser.email = kcUser.email;
			dbUser.persist();
		}

		var addedGroups = diff(keycloakGroups.keySet(), databaseGroups.keySet());
		for (var id : addedGroups) {
			keycloakGroups.get(id).persist();
		}

		var deletedGroups = diff(databaseGroups.keySet(), keycloakGroups.keySet());
		for (var id : deletedGroups) {
			databaseGroups.get(id).delete();
		}

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