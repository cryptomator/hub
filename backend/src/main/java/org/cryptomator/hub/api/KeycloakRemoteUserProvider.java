package org.cryptomator.hub.api;

import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.api.RemoteUserProvider;
import org.cryptomator.hub.api.SyncerConfig;
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

	@Override
	public Stream<User> usersIncludingGroups() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			var groups = groupsIncludingMembers().collect(Collectors.toSet());
			return keycloak.realm(syncerConfig.getKeycloakRealm()).users().list().stream().map(user -> {
				var actualGroups = groups.stream().filter(group -> group.members.stream().anyMatch(usr -> usr.id.equals(user.getId()))).collect(Collectors.toSet());
				return mapToUser(user, Optional.of(actualGroups));
			});
		}
	}

	private User mapToUser(UserRepresentation userRepresentation) {
		return mapToUser(userRepresentation, Optional.empty());
	}

	private User mapToUser(UserRepresentation userRepresentation, Optional<Set<Group>> groups) {
		var userEntity = new User();
		userEntity.id = userRepresentation.getId();
		userEntity.name = userRepresentation.getUsername();
		userEntity.email = userRepresentation.getEmail();
		getPictureUrl(userRepresentation.getAttributes()).ifPresent(it -> userEntity.pictureUrl = it);
		groups.ifPresent(it -> userEntity.groups = it);
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
			var groups = keycloak.realm(syncerConfig.getKeycloakRealm()).groups().groups().stream().map(group -> {
				var members  = keycloak.realm(syncerConfig.getKeycloakRealm()).groups().group(group.getId()).members().stream().map(this::mapToUser).collect(Collectors.toSet());
				// at this point, members contains only this group and not all groups. all other groups will be added to this user in a second step
				return mapToGroup(group, members);
			}).toList();

			return groups.stream().map(group -> {
				var actualMembers = group.members.stream().map(member -> {
					var actualGroups = groups.stream().filter(grp -> grp.members.contains(member)).collect(Collectors.toSet());
					return setGroups(member, actualGroups);
				}).collect(Collectors.toSet());
				return setMembers(group, actualMembers);
			});

		}
	}

	private Group mapToGroup(GroupRepresentation group, Set<User> member) {
		var groupEntity = new Group();
		groupEntity.id = group.getId();
		groupEntity.name = group.getName();
		groupEntity.members = member;
		return groupEntity;
	}

	private Group setMembers(Group group, Set<User> member) {
		var groupEntity = new Group();
		groupEntity.id = group.id;
		groupEntity.name = group.name;
		groupEntity.members = member;
		return groupEntity;
	}

	private User setGroups(User user, Set<Group> groups) {
		var userEntity = new User();
		userEntity.id = user.id;
		userEntity.name = user.name;
		userEntity.email = user.email;
		userEntity.pictureUrl = user.pictureUrl;
		userEntity.groups = groups;
		return userEntity;
	}

	@Override
	public Stream<Group> searchGroup(String groupname) {
		return groups().filter(group -> group.name.toLowerCase().contains(groupname.toLowerCase()));
	}
}