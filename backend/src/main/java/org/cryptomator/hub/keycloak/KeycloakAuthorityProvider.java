package org.cryptomator.hub.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
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

	private static final Logger LOG = Logger.getLogger(KeycloakAuthorityProvider.class);

	//visible for testing
	static final int MAX_COUNT_PER_REQUEST = 5_000;

	private final Keycloak keycloak;
	private final String keycloakRealm;

	@Inject
	KeycloakAuthorityProvider(Keycloak keycloak, @ConfigProperty(name = "hub.keycloak.realm") String keycloakRealm) {
		this.keycloak = keycloak;
		this.keycloakRealm = keycloakRealm;
	}

	public List<KeycloakUserDto> users() {
		return users(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<KeycloakUserDto> users(RealmResource realm) {
		List<KeycloakUserDto> users = new ArrayList<>();
		List<KeycloakUserDto> currentRequestedUsers;

		try {
			do {
				currentRequestedUsers = realm.users().list(users.size(), MAX_COUNT_PER_REQUEST).stream().map(KeycloakAuthorityProvider::mapToUser).toList();
				users.addAll(currentRequestedUsers);
			} while (currentRequestedUsers.size() == MAX_COUNT_PER_REQUEST);

			var cliUser = cryptomatorCliUser(realm);
			cliUser.ifPresent(users::add);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to read users from Keycloak (offset {0}).", users.size());
			throw e;
		}

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

	static KeycloakUserDto mapToUser(UserRepresentation userRepresentation) {
		var pictureUrl = parsePictureUrl(userRepresentation.getAttributes());
		return new KeycloakUserDto(userRepresentation.getId(),
				userRepresentation.getUsername(),
				userRepresentation.getEmail(),
				userRepresentation.getFirstName(),
				userRepresentation.getLastName(),
				pictureUrl,
				userRepresentation.isEnabled());
	}

	static String parsePictureUrl(Map<String, List<String>> attributes) {
		if (attributes != null && attributes.containsKey("picture")) {
			var pictures = attributes.get("picture");
			return pictures.stream().findFirst().orElse(null);
		} else {
			return null;
		}
	}

	public List<KeycloakGroupDto> groups() {
		return groups(keycloak.realm(keycloakRealm));
	}

	//visible for testing
	List<KeycloakGroupDto> groups(RealmResource realm) {
		try {
			return deepCollectGroups(realm).stream().map(group -> {
				var pictureUrl = parsePictureUrl(group.getAttributes());
				// TODO add sub groups and the members of the sub group to it too using `group.getSubGroups()` recursively
				var members = deepCollectMembers(realm, group.getId());
				return new KeycloakGroupDto(group.getId(), group.getName(), pictureUrl, members);
			}).toList();
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to read groups from Keycloak.");
			throw e;
		}
	}

	private List<GroupRepresentation> deepCollectGroups(RealmResource realm) {
		var group = realm.groups();

		List<GroupRepresentation> groups = new ArrayList<>();
		List<GroupRepresentation> currentRequestedGroups;

		do {
			currentRequestedGroups = group.groups(null, groups.size(), MAX_COUNT_PER_REQUEST, false);
			groups.addAll(currentRequestedGroups);
		} while (currentRequestedGroups.size() == MAX_COUNT_PER_REQUEST);

		return groups;
	}

	private Set<KeycloakUserDto> deepCollectMembers(RealmResource realm, String groupId) {
		var group = realm.groups().group(groupId);

		List<UserRepresentation> members = new ArrayList<>();
		List<UserRepresentation> currentRequestedMemebers;

		try {
			do {
				currentRequestedMemebers = group.members(members.size(), MAX_COUNT_PER_REQUEST);
				members.addAll(currentRequestedMemebers);
			} while (currentRequestedMemebers.size() == MAX_COUNT_PER_REQUEST);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to read members of group {0} from Keycloak (offset {1}).", groupId, members.size());
			throw e;
		}

		return members.stream().map(KeycloakAuthorityProvider::mapToUser).collect(Collectors.toSet());
	}

	public List<UserRepresentation> usersInRole(RealmRole role) {
		return usersInRole(keycloak.realm(keycloakRealm), role.kcName());
	}

	//visible for testing
	List<UserRepresentation> usersInRole(RealmResource realm, String roleName) {
		var roles = realm.roles();

		List<UserRepresentation> users = new ArrayList<>();
		List<UserRepresentation> currentBatch;

		try {
			do {
				currentBatch = roles.get(roleName).getUserMembers(true, users.size(), MAX_COUNT_PER_REQUEST);
				users.addAll(currentBatch);
			} while (currentBatch.size() == MAX_COUNT_PER_REQUEST);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to read users in role {0} from Keycloak (offset {1}).", roleName, users.size());
			throw e;
		}

		return users;
	}
}