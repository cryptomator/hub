package org.cryptomator.hub;

import io.quarkus.scheduler.Scheduled;
import org.cryptomator.hub.config.HubConfig;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.keycloak.admin.client.Keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class UserProvider {

	// TODO provide button to trigger sync?

	private static final String realm = "cryptomator";

	@Inject
	HubConfig config;

	@Scheduled(every="1m") // TODO use in final version Scheduled variant loaded from config
	void sync() {
		try(Keycloak keycloak = Keycloak.getInstance(config.getOidcAuthEndpoint(), realm, "syncer", "syncer", "admin-cli")) {
			updateUsers(keycloak);
			updateGroups(keycloak);
		}
	}

	private void updateGroups(Keycloak keycloak) {
		var keycloakGroups = keycloakGroups(keycloak).collect(Collectors.toSet());
		var databaseGroups = databaseGroups().collect(Collectors.toSet());

		var newGroups = keycloakGroups.stream().filter(keycloakGroup -> !databaseGroups.contains(keycloakGroup)).collect(Collectors.toSet());
		var removedGroups = databaseGroups.stream().filter(databaseGroup -> !keycloakGroups.contains(databaseGroup)).collect(Collectors.toSet());

		for(Group group: newGroups) {
			Group.createOrUpdate(group.id, group.name);
		}

		for(Group group: removedGroups) {
			if(!Group.deleteById(group.id)) {
				System.out.println("Failed to delete group: ${user.id}");
			}
		}
	}

	private void updateUsers(Keycloak keycloak) {
		var keycloakUsers = keycloakUsers(keycloak).collect(Collectors.toSet());
		var databaseUsers = databaseUsers().collect(Collectors.toSet());

		var newOrUpdatedUsers = keycloakUsers.stream().filter(keycloakUser -> !databaseUsers.contains(keycloakUser)).collect(Collectors.toSet());
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

	private Stream<User> keycloakUsers(Keycloak keycloak) {
		return keycloak.realm(realm).users().list().stream().map(user -> {
			var groups = keycloak.realms().realm(realm).users().get(user.getId()).groups().stream().map(group -> {
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				return groupEntity;
			}).collect(Collectors.toSet());

			var pictureUrl = getPictureUrl(user.getAttributes());

			var userEntity = new User();
			userEntity.id = user.getId();
			userEntity.name = user.getUsername();
			userEntity.email = user.getEmail();
			userEntity.groups = groups;
			pictureUrl.ifPresent(it -> userEntity.pictureUrl = it);
			return userEntity;
		});
	}

	private Optional<String> getPictureUrl(Map<String, List<String>> attributes) {
		try {
			return Optional.ofNullable(attributes.get("picture").get(0));
		} catch (NullPointerException e) {
			return Optional.empty();
		}
	}

	private Stream<Group> keycloakGroups(Keycloak keycloak) {
		return keycloak.realm(realm).groups().groups().stream().map(group -> {
			var groupEntity = new Group();
			groupEntity.id = group.getId();
			groupEntity.name = group.getName();
			return groupEntity;
		});
	}
}