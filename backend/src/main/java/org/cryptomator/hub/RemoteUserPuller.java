package org.cryptomator.hub;

import io.quarkus.scheduler.Scheduled;
import org.cryptomator.hub.entities.Access;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class RemoteUserPuller {

	@Inject
	SyncerConfig config;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	@Transactional
	void sync() {
		var keycloakGroups = new RemoteUserProviderFactory().get(config).groups().collect(Collectors.toSet());
		var databaseGroups = databaseGroups().collect(Collectors.toSet());
		var keycloakUsers = new RemoteUserProviderFactory().get(config).usersIncludingGroups().collect(Collectors.toSet());
		var databaseUsers = databaseUsers().collect(Collectors.toSet());

		var newOrUpdatedGroups = newOrUpdatedGroups(keycloakGroups, databaseGroups);
		var removedGroups = getRemovedGroups(keycloakGroups, databaseGroups);
		var newUserOrUpdatedUsersMetadata = getNewUserOrUpdatedUsersMetadata(keycloakUsers, databaseUsers);
		var updatedGroupMembers = getUsersWithUpdatedGroupMembers(keycloakUsers, databaseUsers);
		var removedUsers = getRemovedUsers(keycloakUsers, databaseUsers);
		var usersWithReducedGroups = getUsersWithReducedGroups(keycloakUsers, databaseUsers);

		for (Group group : newOrUpdatedGroups) {
			Group.createOrUpdate(group.id, group.name);
		}

		for (User user : newUserOrUpdatedUsersMetadata) {
			User.createOrUpdate(user.id, user.name, user.pictureUrl, user.email);
		}

		for (User user : updatedGroupMembers) {
			User.createOrUpdate(user.id, user.name, user.pictureUrl, user.email, user.groups);
		}

		for (User user : removedUsers) {
			if (!User.deleteById(user.id)) {
				System.out.printf("Failed to delete user %s%n", user.id);
			}
		}

		if(!removedGroups.isEmpty()) {
			var removedGroupIds = removedGroups.stream().map(group -> group.id).collect(Collectors.toSet());
			Access.revokeDeviceAccessForGroupsIfNoAccessViaUserGranted(removedGroupIds);
		}

		for (Group group : removedGroups) {
			if (!Group.deleteById(group.id)) {
				System.out.printf("Failed to delete group %s%n", group.id);
			}
		}

		if(!usersWithReducedGroups.isEmpty()) {
			var usersWithReducedGroupsIds = usersWithReducedGroups.stream().map(user -> user.id).collect(Collectors.toSet());
			Access.revokeDeviceAccessForUsersIfNoAccessViaGroupsGranted(usersWithReducedGroupsIds);
		}
	}

	Set<Group> newOrUpdatedGroups(Set<Group> keycloakGroups, Set<Group> databaseGroups) {
		return keycloakGroups
				.stream()
				.filter(keycloakGroup -> !databaseGroups.contains(keycloakGroup) || databaseGroups.stream().noneMatch(group -> group.id.equals(keycloakGroup.id)))
				.collect(Collectors.toSet());
	}

	Set<Group> getRemovedGroups(Set<Group> keycloakGroups, Set<Group> databaseGroups) {
		return databaseGroups
				.stream()
				.filter(databaseGroup -> keycloakGroups.stream().noneMatch(keycloakGroup -> databaseGroup.id.equals(keycloakGroup.id)))
				.collect(Collectors.toSet());
	}

	Set<User> getNewUserOrUpdatedUsersMetadata(Set<User> keycloakUsers, Set<User> databaseUsers) {
		return keycloakUsers
				.stream()
				.filter(keycloakUser -> !databaseUsers.contains(keycloakUser) || databaseUsers.stream().noneMatch(user -> user.id.equals(keycloakUser.id)))
				.collect(Collectors.toSet());
	}

	Set<User> getUsersWithUpdatedGroupMembers(Set<User> keycloakUsers, Set<User> databaseUsers) {
		return keycloakUsers.stream().filter(keycloakUser -> {
			var dbUser = databaseUsers.stream().filter(user -> user.id.equals(keycloakUser.id)).findAny();
			return dbUser.isPresent() && !keycloakUser.groups.equals(dbUser.get().groups);
		}).collect(Collectors.toSet());
	}

	Set<User> getRemovedUsers(Set<User> keycloakUsers, Set<User> databaseUsers) {
		return databaseUsers
				.stream()
				.filter(databaseUser -> keycloakUsers.stream().noneMatch(keycloakUser -> databaseUser.id.equals(keycloakUser.id)))
				.collect(Collectors.toSet());
	}

	Set<User> getUsersWithReducedGroups(Set<User> keycloakUsers, Set<User> databaseUsers) {
		return keycloakUsers.stream().filter(keycloakUser -> {
			var dbUser = databaseUsers.stream().filter(user -> user.id.equals(keycloakUser.id)).findAny();
			return dbUser.isPresent() && dbUser.get().groups.stream().anyMatch(dbGroup -> keycloakUser.groups.stream().noneMatch(groupId -> groupId.id.equals(dbGroup.id)));
		}).collect(Collectors.toSet());
	}

	private Stream<User> databaseUsers() {
		return User.findAll().stream();
	}

	private Stream<Group> databaseGroups() {
		return Group.findAll().stream();
	}

}