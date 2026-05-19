package org.cryptomator.hub.keycloak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.events.VaultAccessChanged;

import java.util.Arrays;
import java.util.HashMap;
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
	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;
	@Inject
	Event<VaultAccessChanged> vaultAccessChangedEvent;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	@WithSpan("KeycloakAuthorityPuller.sync")
	void sync() {
		var keycloakGroups = remoteUserProvider.groups().stream().collect(Collectors.toMap(KeycloakGroupDto::id, Function.identity()));
		var keycloakUsers = remoteUserProvider.users().stream().collect(Collectors.toMap(KeycloakUserDto::id, Function.identity()));
		var keycloakRealmRoles = Arrays.stream(RealmRole.values()).collect(Collectors.toMap(Function.identity(), remoteUserProvider::usersInRole));
		for (var role : RealmRole.values()) {
			var usersInRole = keycloakRealmRoles.get(role);
			for (var user : usersInRole) {
				var keycloakUser = keycloakUsers.get(user.getId());
				if (keycloakUser != null) {
					keycloakUser.roles().add(role);
				}
			}
		}
		sync(keycloakGroups, keycloakUsers);
	}

	@Transactional
	void sync(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, KeycloakUserDto> keycloakUsers) {
		// get current state from database:
		var databaseUsers = userRepo.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));
		var databaseGroups = groupRepo.findAll().stream().collect(Collectors.toMap(Group::getId, Function.identity()));

		// sync users:
		var addedUsers = syncAddedUsers(keycloakUsers, databaseUsers);
		var deletedUserIds = syncDeletedUsers(keycloakUsers, databaseUsers);
		syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUserIds);

		// all users after additions and deletions:
		Map<String, Authority> allAuthorities = merge(databaseUsers, addedUsers);
		deletedUserIds.forEach(allAuthorities::remove);

		// sync groups:
		var addedGroups = syncAddedGroups(keycloakGroups, databaseGroups, allAuthorities);
		var deletedGroupIds = syncDeletedGroups(keycloakGroups, databaseGroups);
		syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroupIds, allAuthorities);

		// Coarse: fire once at the end of a successful sync. Observers (e.g. the automatic access grant long-poller) will
		// re-query the database to determine what actually changed. Group-membership updates inside syncUpdatedUsers /
		// syncUpdatedGroups are not tracked individually, so firing unconditionally keeps the broadcaster correct.
		vaultAccessChangedEvent.fire(new VaultAccessChanged());
	}

	//visible for testing
	Map<String, User> syncAddedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers) {
		var addedIds = diff(keycloakUsers.keySet(), databaseUsers.keySet());
		var added = addedIds.stream().map(id -> {
			var keycloakUser = keycloakUsers.get(id);
			var databaseUser = new User();
			databaseUser.setId(keycloakUser.id());
			databaseUser.setName(keycloakUser.name());
			databaseUser.setEmail(keycloakUser.email());
			databaseUser.setFirstName(keycloakUser.firstName());
			databaseUser.setLastName(keycloakUser.lastName());
			databaseUser.setPictureUrl(keycloakUser.pictureUrl());
			databaseUser.setEnabled(keycloakUser.enabled());
			databaseUser.setRealmRoles(keycloakUser.roles().stream().map(RealmRole::kcName).toArray(String[]::new));
			return databaseUser;
		}).collect(Collectors.toMap(User::getId, Function.identity()));
		userRepo.persist(added.values());
		effectiveGroupMembershipRepo.updateUsers(addedIds);
		return added;
	}

	//visible for testing
	Set<String> syncDeletedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers) {
		var deletedIds = diff(databaseUsers.keySet(), keycloakUsers.keySet());
		userRepo.deleteByIds(deletedIds);
		effectiveGroupMembershipRepo.updateUsers(deletedIds);
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
			databaseUser.setFirstName(keycloakUser.firstName());
			databaseUser.setLastName(keycloakUser.lastName());
			databaseUser.setPictureUrl(keycloakUser.pictureUrl());
			databaseUser.setEnabled(keycloakUser.enabled());
			databaseUser.setRealmRoles(keycloakUser.roles().stream().map(RealmRole::kcName).toArray(String[]::new));
		}
	}

	//visible for testing
	Map<String, Group> syncAddedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups, Map<String, Authority> allAuthorities) {
		var addedIds = diff(keycloakGroups.keySet(), databaseGroups.keySet());
		var added = addedIds.stream().map(id -> {
			var keycloakGroup = keycloakGroups.get(id);
			var databaseGroup = new Group();
			databaseGroup.setId(keycloakGroup.id());
			databaseGroup.setName(keycloakGroup.name());
			databaseGroup.setPictureUrl(keycloakGroup.pictureUrl());
			databaseGroup.getMembers().addAll(keycloakGroup.members().stream().map(KeycloakUserDto::id).map(allAuthorities::get).collect(Collectors.toSet()));
			return databaseGroup;
		}).collect(Collectors.toMap(Group::getId, Function.identity()));
		groupRepo.persist(added.values());
		effectiveGroupMembershipRepo.updateGroups(addedIds);
		return added;
	}

	//visible for testing
	Set<String> syncDeletedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups) {
		var deletedIds = diff(databaseGroups.keySet(), keycloakGroups.keySet());
		groupRepo.deleteByIds(deletedIds);
		effectiveGroupMembershipRepo.updateGroups(deletedIds);
		return deletedIds;
	}

	//visible for testing
	void syncUpdatedGroups(Map<String, KeycloakGroupDto> keycloakGroups, Map<String, Group> databaseGroups, Set<String> deletedGroupIds, Map<String, Authority> allAuthorities) {
		var toUpdateIds = diff(databaseGroups.keySet(), deletedGroupIds);
		var idsOfGroupsWithChangedMembers = new HashSet<String>();
		for (var id : toUpdateIds) {
			var databaseGroup = databaseGroups.get(id);
			var keycloakGroup = keycloakGroups.get(id);
			databaseGroup.setName(keycloakGroup.name());
			databaseGroup.setPictureUrl(keycloakGroup.pictureUrl());

			// update members:
			var kcMemberIds = keycloakGroup.members().stream().map(KeycloakUserDto::id).collect(Collectors.toSet());
			var dbMemberIds = databaseGroup.getMembers().stream().map(Authority::getId).collect(Collectors.toSet());
			var addedMemberIds = diff(kcMemberIds, dbMemberIds);
			var addedMembers = addedMemberIds.stream().map(allAuthorities::get).collect(Collectors.toSet());
			databaseGroup.getMembers().addAll(addedMembers);
			var removedMemberIds = diff(dbMemberIds, kcMemberIds);
			databaseGroup.getMembers().removeIf(u -> removedMemberIds.contains(u.getId()));
			if (!addedMemberIds.isEmpty() || !removedMemberIds.isEmpty()) {
				idsOfGroupsWithChangedMembers.add(id);
			}
		}
		effectiveGroupMembershipRepo.updateGroups(idsOfGroupsWithChangedMembers);
	}

	private static <T> Set<T> diff(Set<T> base, Set<T> difference) {
		var result = new HashSet<>(base);
		result.removeAll(difference);
		return result;
	}

	private static <K, V> Map<K, V> merge(Map<K, ? extends V> first, Map<K, ? extends V> second) {
		Map<K, V> result = new HashMap<>(first);
		result.putAll(second);
		return result;
	}
}
