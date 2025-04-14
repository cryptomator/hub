package org.cryptomator.hub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakRemoteUserProvider implements RemoteUserProvider {

	//visible for testing
	static final int MAX_COUNT_PER_REQUEST = 5_000;

	@Inject
	Keycloak keycloak;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	@Override
	public List<User> users() {
		return users(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<User> users(RealmResource realm) {
		List<User> users = new ArrayList<>();
		List<User> currentRequestedUsers;

		do {
			currentRequestedUsers = realm.users().list(users.size(), MAX_COUNT_PER_REQUEST).stream().map(this::mapToUser).toList();
			users.addAll(currentRequestedUsers);
		} while (currentRequestedUsers.size() == MAX_COUNT_PER_REQUEST);

		var cliUser = cryptomatorCliUser(realm);
		cliUser.ifPresent(users::add);

		return users;
	}

	//visible for testing
	Optional<User> cryptomatorCliUser(RealmResource realm) {
		var clients = realm.clients().findByClientId("cryptomatorhub-cli");
		if (clients.isEmpty()) {
			return Optional.empty();
		}
		var clientId = clients.get(0).getId();
		var client = realm.clients().get(clientId);
		var clientUser = client.getServiceAccountUser();
		return Optional.of(mapToUser(clientUser));
	}

	private User mapToUser(UserRepresentation userRepresentation) {
		var userEntity = new User();
		userEntity.setId(userRepresentation.getId());
		userEntity.setName(userRepresentation.getUsername());
		userEntity.setEmail(userRepresentation.getEmail());
		parsePictureUrl(userRepresentation.getAttributes()).ifPresent(userEntity::setPictureUrl);
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
	public List<Group> groups() {
		return groups(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<Group> groups(RealmResource realm) {
		return deepCollectGroups(realm).stream().map(group -> {
			// TODO add sub groups and the members of the sub group to it too using `group.getSubGroups()` recursively
			var members = deepCollectMembers(realm, group.getId());
			var groupEntity = new Group();
			groupEntity.setId(group.getId());
			groupEntity.setName(group.getName());
			groupEntity.setMembers(members);
			return groupEntity;
		}).toList();
	}

	private List<GroupRepresentation> deepCollectGroups(RealmResource realm) {
		var group = realm.groups();

		List<GroupRepresentation> groups = new ArrayList<>();
		List<GroupRepresentation> currentRequestedGroups;

		do {
			currentRequestedGroups = group.groups(groups.size(), MAX_COUNT_PER_REQUEST);
			groups.addAll(currentRequestedGroups);
		} while (currentRequestedGroups.size() == MAX_COUNT_PER_REQUEST);

		return groups;
	}

	private Set<Authority> deepCollectMembers(RealmResource realm, String groupId) {
		var group = realm.groups().group(groupId);

		List<UserRepresentation> members = new ArrayList<>();
		List<UserRepresentation> currentRequestedMemebers;

		do {
			currentRequestedMemebers = group.members(members.size(), MAX_COUNT_PER_REQUEST);
			members.addAll(currentRequestedMemebers);
		} while (currentRequestedMemebers.size() == MAX_COUNT_PER_REQUEST);

		return members.stream().map(this::mapToUser).collect(Collectors.toSet());
	}
}