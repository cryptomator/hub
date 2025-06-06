package org.cryptomator.hub.keycloak;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakAuthorityPuller {

	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	KeycloakAuthorityProvider remoteUserProvider;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	void sync() {
		var keycloakGroups = remoteUserProvider.groups().stream().collect(Collectors.toMap(KeycloakGroupDto::id, Function.identity()));
		var keycloakUsers = remoteUserProvider.users().stream().collect(Collectors.toMap(KeycloakUserDto::id, Function.identity()));
		sync(keycloakGroups, keycloakUsers);
	}

	@Transactional
	void sync(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, KeycloakUserDto> keycloakUsers) {
		var databaseUsers = userRepo.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
		var databaseGroups = groupRepo.findAll().stream().collect(Collectors.toMap(Group::getId, Function.identity()));
		syncAddedUsers(keycloakUsers, databaseUsers);
		var deletedUserIds = syncDeletedUsers(keycloakUsers, databaseUsers);
		syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUserIds);
		syncAddedGroups(keycloakGroups, databaseGroups, databaseUsers);
		var deletedGroupIds = syncDeletedGroups(keycloakGroups, databaseGroups);
		syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroupIds, databaseUsers);
	}

	//visible for testing
	void syncAddedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers) {
		var addedIds = diff(keycloakUsers.keySet(), databaseUsers.keySet());
		for (var id : addedIds) {
			var keycloakUser = keycloakUsers.get(id);
			var databaseUser = new User();
			databaseUser.setId(keycloakUser.id());
			databaseUser.setName(keycloakUser.name());
			databaseUser.setEmail(keycloakUser.email());
			databaseUser.setPictureUrl(keycloakUser.pictureUrl());
			userRepo.persist(databaseUser);
		}
	}

	//visible for testing
	Set<String> syncDeletedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers) {
		var deletedIds = diff(databaseUsers.keySet(), keycloakUsers.keySet());
		for (var id : deletedIds) {
			userRepo.delete(databaseUsers.get(id));
		}
		return deletedIds;
	}

	//visible for testing
	void syncUpdatedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers, Set<String> deletedUserIds) {
		var toUpdateIds = diff(databaseUsers.keySet(), deletedUserIds);
		for (var id : toUpdateIds) {
			var databaseUser = databaseUsers.get(id);
			var keycloakUser = keycloakUsers.get(id);
			databaseUser.setName(keycloakUser.name());
			databaseUser.setEmail(keycloakUser.email());
			databaseUser.setPictureUrl(keycloakUser.pictureUrl());
		}
	}

	//visible for testing
	void syncAddedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups, Map<String, User> databaseUsers) {
		var addedIds = diff(keycloakGroups.keySet(), databaseGroups.keySet());
		for (var id : addedIds) {
			var keycloakGroup = keycloakGroups.get(id);
			var databaseGroup = new Group();
			databaseGroup.setId(keycloakGroup.id());
			databaseGroup.setName(keycloakGroup.name());
			Set<Authority> members = new HashSet<>();
			for (var keycloakMember : keycloakGroup.members()) {
				var databaseUser = databaseUsers.get(keycloakMember.id());
				members.add(databaseUser);
			}
			databaseGroup.setMembers(members);
			groupRepo.persist(databaseGroup);
		}
	}

	//visible for testing
	Set<String> syncDeletedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups) {
		var deletedIds = diff(databaseGroups.keySet(), keycloakGroups.keySet());
		for (var id : deletedIds) {
			var databaseGroup = databaseGroups.get(id);
			groupRepo.delete(databaseGroup);
		}
		return deletedIds;
	}

	//visible for testing
	void syncUpdatedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups, Set<String> deletedGroupIds, Map<String, User> databaseUsers) {
		var toUpdateIds = diff(databaseGroups.keySet(), deletedGroupIds);
		for (var id : toUpdateIds) {
			var databaseGroup = databaseGroups.get(id);
			var keycloakGroup = keycloakGroups.get(id);
			var wantIds = keycloakGroup.members().stream().map(KeycloakUserDto::id).collect(Collectors.toSet());
			var haveIds = databaseGroup.getMembers().stream().map(Authority::getId).collect(Collectors.toSet());
			databaseGroup.setName(keycloakGroup.name());
			for (var addId : diff(wantIds, haveIds)) {
				var databaseUser = databaseUsers.get(addId);
				databaseGroup.getMembers().add(databaseUser);
			}
			for (var removeId : diff(haveIds, wantIds)) {
				databaseGroup.getMembers().removeIf(u -> u.getId().equals(removeId));
			}
		}
	}

	private <T> Set<T> diff(Set<T> base, Set<T> difference) {
		var result = new HashSet<>(base);
		result.removeAll(difference);
		return result;
	}
}
