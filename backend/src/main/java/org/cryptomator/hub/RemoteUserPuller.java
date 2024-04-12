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
	Authority.Repository authorityRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	User.Repository userRepo;
	@Inject
	RemoteUserProvider remoteUserProvider;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	void sync() {
		var keycloakGroups = remoteUserProvider.groups().stream().collect(Collectors.toMap(Authority::getId, Function.identity()));
		var keycloakUsers = remoteUserProvider.users().stream().collect(Collectors.toMap(Authority::getId, Function.identity()));
		sync(keycloakGroups, keycloakUsers);
	}

	@Transactional
	void sync(Map<String, Group> keycloakGroups, Map<String, User> keycloakUsers) {
		var databaseGroups = groupRepo.findAll().stream().collect(Collectors.toMap(Authority::getId, Function.identity()));
		var databaseUsers = userRepo.findAll().stream().collect(Collectors.toMap(Authority::getId, Function.identity()));
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
			authorityRepo.persist(keycloakAuthorities.get(id));
		}
	}

	//visible for testing
	<T extends Authority> Set<String> syncDeletedAuthorities(Map<String, T> keycloakAuthorities, Map<String, T> databaseAuthorities) {
		var deletedAuthorities = diff(databaseAuthorities.keySet(), keycloakAuthorities.keySet());
		for (var id : deletedAuthorities) {
			authorityRepo.delete(keycloakAuthorities.get(id));
		}
		return deletedAuthorities;
	}

	//visible for testing
	void syncUpdatedUsers(Map<String, User> keycloakUsers, Map<String, User> databaseUsers, Set<String> deletedUsers) {
		var updatedUsers = diff(databaseUsers.keySet(), deletedUsers);
		for (var id : updatedUsers) {
			var dbUser = databaseUsers.get(id);
			var kcUser = keycloakUsers.get(id);
			dbUser.setPictureUrl(kcUser.getPictureUrl());
			dbUser.setName(kcUser.getName());
			dbUser.setEmail(kcUser.getEmail());
			userRepo.persist(dbUser);
		}
	}

	//visible for testing
	void syncUpdatedGroups(Map<String, Group> keycloakGroups, Map<String, Group> databaseGroups, Set<String> deletedGroups) {
		var updatedGroups = diff(databaseGroups.keySet(), deletedGroups);
		for (var id : updatedGroups) {
			var dbGroup = databaseGroups.get(id);
			var kcGroup = keycloakGroups.get(id);

			dbGroup.setName(kcGroup.getName());

			dbGroup.getMembers().addAll(diff(kcGroup.getMembers(), dbGroup.getMembers()));
			dbGroup.getMembers().removeAll(diff(dbGroup.getMembers(), kcGroup.getMembers()));
			// TODO why don't we run dbGroup.persist()?
		}
	}

	private <T> Set<T> diff(Set<T> base, Set<T> difference) {
		var result = new HashSet<>(base);
		result.removeAll(difference);
		return result;
	}

}