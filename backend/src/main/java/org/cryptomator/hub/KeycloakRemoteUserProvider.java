package org.cryptomator.hub;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class KeycloakRemoteUserProvider implements RemoteUserProvider {

	private final SyncerConfig syncerConfig;

	public KeycloakRemoteUserProvider(SyncerConfig syncerConfig) {
		this.syncerConfig = syncerConfig;
	}

	@Override
	public Stream<User> users() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().list().stream().map(this::mapToUser);
		}
	}

	private User mapToUser(UserRepresentation userRepresentation) {
		var userEntity = new User();
		userEntity.id = userRepresentation.getId();
		userEntity.name = userRepresentation.getUsername();
		userEntity.email = userRepresentation.getEmail();
		getPictureUrl(userRepresentation.getAttributes()).ifPresent(it -> userEntity.pictureUrl = it);
		return userEntity;
	}

	private Optional<String> getPictureUrl(Map<String, List<String>> attributes) {
		try {
			return Optional.ofNullable(attributes.get("picture").get(0));
		} catch (NullPointerException e) {
			return Optional.empty();
		}
	}

	@Override
	public Stream<User> searchUser(String querry) {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().search(querry).stream().map(this::mapToUser);
		}
	}

	@Override
	public Stream<Group> groups() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).groups().groups().stream().map(group -> {
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				return groupEntity;
			});
		}
	}

	@Override
	public Stream<Group> groupsIncludingMembers() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).groups().groups().stream().map(group -> {
				var members  = keycloak.realm(syncerConfig.getKeycloakRealm()).groups().group(group.getId()).members().stream().<Authority>map(this::mapToUser).collect(Collectors.toSet());
				return mapToGroup(group, members);
			});
		}
	}

	private Group mapToGroup(GroupRepresentation group, Set<Authority> member) {
		var groupEntity = new Group();
		groupEntity.id = group.getId();
		groupEntity.name = group.getName();
		groupEntity.members = member;
		return groupEntity;
	}

	@Override
	public Stream<Group> searchGroup(String groupname) {
		return groups().filter(group -> group.name.toLowerCase().contains(groupname.toLowerCase()));
	}
}