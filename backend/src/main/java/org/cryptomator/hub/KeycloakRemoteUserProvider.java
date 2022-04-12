package org.cryptomator.hub;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakRemoteUserProvider implements RemoteUserProvider {

	private final SyncerConfig syncerConfig;

	public KeycloakRemoteUserProvider(SyncerConfig syncerConfig) {
		this.syncerConfig = syncerConfig;
	}

	@Override
	public List<User> users() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().list().stream().map(this::mapToUser).toList();
		}
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
	public List<User> searchUser(String querry) {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().search(querry).stream().map(this::mapToUser).toList();
		}
	}

	@Override
	public List<Group> groups() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).groups().groups().stream().map(group -> {
				// TODO add sub groups and the members of the sub group to it too
				var members  = keycloak.realm(syncerConfig.getKeycloakRealm()).groups().group(group.getId()).members().stream().<Authority>map(this::mapToUser).collect(Collectors.toSet());
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				groupEntity.members = members;
				return groupEntity;
			}).toList();
		}
	}

	@Override
	public List<Group> searchGroup(String groupname) {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			return keycloak.realm(syncerConfig.getKeycloakRealm()).groups().groups().stream().map(group -> {
				var groupEntity = new Group();
				groupEntity.id = group.getId();
				groupEntity.name = group.getName();
				return groupEntity;
			}).filter(group -> group.name.toLowerCase().contains(groupname.toLowerCase())).toList();
		}
	}
}