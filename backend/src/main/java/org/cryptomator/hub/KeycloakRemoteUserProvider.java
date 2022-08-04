package org.cryptomator.hub;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakRemoteUserProvider implements RemoteUserProvider {

	private static final int MAX_COUNT_PER_REQUEST = 5_000;

	@Inject
	SyncerConfig syncerConfig;

	@Override
	public List<User> users() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			List<User> users = new ArrayList<>();
			List<User> currentRequestedUsers;

			do {
				currentRequestedUsers = keycloak.realm(syncerConfig.getKeycloakRealm()).users().list(users.size(), MAX_COUNT_PER_REQUEST).stream().filter(notSyncerUser()).map(this::mapToUser).toList();
				users.addAll(currentRequestedUsers);
			} while (currentRequestedUsers.size() == MAX_COUNT_PER_REQUEST);

			return users;
		}
	}

	private Predicate<UserRepresentation> notSyncerUser() {
		return user -> !user.getUsername().equals(syncerConfig.getUsername());
	}

	private User mapToUser(UserRepresentation userRepresentation) {
		var userEntity = new User();
		userEntity.id = userRepresentation.getId();
		userEntity.name = userRepresentation.getUsername();
		userEntity.email = userRepresentation.getEmail();
		parsePictureUrl(userRepresentation.getAttributes()).ifPresent(it -> userEntity.pictureUrl = it);
		return userEntity;
	}

	private Optional<String> parsePictureUrl(Map<String, List<String>> attributes) {
		try {
			return Optional.ofNullable(attributes.get("picture").get(0));
		} catch (NullPointerException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<User> searchUser(String query) {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().search(query).stream().filter(notSyncerUser()).map(this::mapToUser).toList();
		}
	}

	@Override
	public List<Group> groups() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return deepCollectGroups(keycloak).stream().map(group -> {
				// TODO add sub groups and the members of the sub group to it too using `group.getSubGroups()` recursively
				var members = deepCollectMembers(keycloak, group.getId());
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				groupEntity.members = members;
				return groupEntity;
			}).toList();
		}
	}

	private List<GroupRepresentation> deepCollectGroups(Keycloak keycloak) {
		var group = keycloak.realm(syncerConfig.getKeycloakRealm()).groups();

		List<GroupRepresentation> groups = new ArrayList<>();
		List<GroupRepresentation> currentRequestedGroups;

		do {
			currentRequestedGroups = group.groups(groups.size(), MAX_COUNT_PER_REQUEST);
			groups.addAll(currentRequestedGroups);
		} while (currentRequestedGroups.size() == MAX_COUNT_PER_REQUEST);

		return groups;
	}

	private Set<Authority> deepCollectMembers(Keycloak keycloak, String groupId) {
		var group = keycloak.realm(syncerConfig.getKeycloakRealm()).groups().group(groupId);

		List<UserRepresentation> members = new ArrayList<>();
		List<UserRepresentation> currentRequestedMemebers;

		do {
			currentRequestedMemebers = group.members(members.size(), MAX_COUNT_PER_REQUEST);
			members.addAll(currentRequestedMemebers);
		} while (currentRequestedMemebers.size() == MAX_COUNT_PER_REQUEST);

		return members.stream().map(this::mapToUser).collect(Collectors.toSet());
	}

	@Override
	public List<Group> searchGroup(String groupname) {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return deepCollectGroups(keycloak).stream().map(group -> {
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				return groupEntity;
			}).filter(group -> group.name.toLowerCase().contains(groupname.toLowerCase())).toList();
		}
	}
}