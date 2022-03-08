package org.cryptomator.hub.spi;

import io.quarkus.scheduler.Scheduled;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class RemoteUserPuller {

	@Inject
	ConfigResource configResource;

	@Scheduled(every="1m") // TODO use in final version Scheduled variant loaded from config
	void sync() {
		updateUsers();
		updateGroups();
	}

	private void updateGroups() {
		var keycloakGroups = RemoteUserProviderFactory.get(configResource).groupsIncludingMembers().collect(Collectors.toSet());
		var databaseGroups = databaseGroups().collect(Collectors.toSet());

		var newOrUpdatedGroups = keycloakGroups.stream().filter(keycloakGroup -> {
			if(databaseGroups.contains(keycloakGroup)) {
				var dbGroup = databaseGroups.stream().filter(group -> group.id.equals(keycloakGroup.id)).findAny().get();
				return !keycloakGroup.equals(dbGroup) || !keycloakGroup.members.equals(dbGroup.members);
			} else {
				return true;
			}
		}).collect(Collectors.toSet());

		var removedGroups = databaseGroups.stream().filter(databaseGroup -> !keycloakGroups.contains(databaseGroup)).collect(Collectors.toSet());

		for(Group group: newOrUpdatedGroups) {
			Group.createOrUpdate(group.id, group.name, group.members);
		}

		for(Group group: removedGroups) {
			if(!Group.deleteById(group.id)) {
				System.out.println("Failed to delete group: ${user.id}");
			}
		}
	}

	private void updateUsers() {
		var keycloakUsers = RemoteUserProviderFactory.get(configResource).usersIncludingGroups().collect(Collectors.toSet());
		var databaseUsers = databaseUsers().collect(Collectors.toSet());

		var newOrUpdatedUsers = keycloakUsers.stream().filter(keycloakUser -> {
			if(databaseUsers.contains(keycloakUser)) {
				var dbUser = databaseUsers.stream().filter(user -> user.id.equals(keycloakUser.id)).findAny().get();
				return !keycloakUser.equals(dbUser) || !keycloakUser.groups.equals(dbUser.groups);
			} else {
				return true;
			}
		}).collect(Collectors.toSet());

		var removedUsers = databaseUsers.stream().filter(databaseUser -> !keycloakUsers.contains(databaseUser)).collect(Collectors.toSet());

		for(User user: newOrUpdatedUsers) {
			User.createOrUpdate(user.id, user.name, user.pictureUrl, user.email, user.groups);
		}

		for(User user: removedUsers) {
			if(!User.deleteById(user.id)) {
				System.out.println("Failed to delete user: ${user.id}");
			}
		}
	}

	private Stream<User> databaseUsers() {
		return User.findAll().stream();
	}

	private Stream<Group> databaseGroups() {
		return Group.findAll().stream();
	}

}