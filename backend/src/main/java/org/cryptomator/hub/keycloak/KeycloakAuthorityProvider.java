package org.cryptomator.hub.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
public class KeycloakAuthorityProvider {

	//visible for testing
	static final int MAX_COUNT_PER_REQUEST = 5_000;

	@Inject
	Keycloak keycloak;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	public List<KeycloakUserDto> users() {
		return users(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<KeycloakUserDto> users(RealmResource realm) {
		List<KeycloakUserDto> users = new ArrayList<>();
		List<KeycloakUserDto> currentRequestedUsers;

		do {
			currentRequestedUsers = realm.users().list(users.size(), MAX_COUNT_PER_REQUEST).stream().map(this::mapToUser).toList();
			users.addAll(currentRequestedUsers);
		} while (currentRequestedUsers.size() == MAX_COUNT_PER_REQUEST);

		var cliUser = cryptomatorCliUser(realm);
		cliUser.ifPresent(users::add);

		return users;
	}

	//visible for testing
	Optional<KeycloakUserDto> cryptomatorCliUser(RealmResource realm) {
		var clients = realm.clients().findByClientId("cryptomatorhub-cli");
		if (clients.isEmpty()) {
			return Optional.empty();
		}
		var clientId = clients.get(0).getId();
		var client = realm.clients().get(clientId);
		var clientUser = client.getServiceAccountUser();
		return Optional.of(mapToUser(clientUser));
	}

	private KeycloakUserDto mapToUser(UserRepresentation userRepresentation) {
		var pictureUrl = parsePictureUrl(userRepresentation.getAttributes());
		return new KeycloakUserDto(userRepresentation.getId(), userRepresentation.getUsername(), userRepresentation.getEmail(), pictureUrl);
	}

	private String parsePictureUrl(Map<String, List<String>> attributes) {
		try {
			return attributes.get("picture").get(0);
		} catch (NullPointerException e) {
			return null;
		}
	}

	public List<KeycloakGroupDto> groups() {
		return groups(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<KeycloakGroupDto> groups(RealmResource realm) {
		return deepCollectGroups(realm).stream().map(group -> {
			// TODO add sub groups and the members of the sub group to it too using `group.getSubGroups()` recursively
			var members = deepCollectMembers(realm, group.getId());
			return new KeycloakGroupDto(group.getId(), group.getName(), members);
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

	private Set<KeycloakUserDto> deepCollectMembers(RealmResource realm, String groupId) {
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