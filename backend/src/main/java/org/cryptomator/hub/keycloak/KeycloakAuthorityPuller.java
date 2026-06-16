package org.cryptomator.hub.keycloak;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.cryptomator.hub.api.ErrorCode;
import org.cryptomator.hub.api.ErrorCodeException;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakAuthorityPuller {

	private static final Logger LOG = Logger.getLogger(KeycloakAuthorityPuller.class);

	@Inject
	Keycloak keycloak;
	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	KeycloakAuthorityProvider remoteUserProvider;
	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;
	@Inject
	KeycloakRealmRoles realmRoles;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	RealmResource realm;

	@PostConstruct
	public void setup() {
		this.realm = keycloak.realm(keycloakRealm);
	}

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	@WithSpan("KeycloakAuthorityPuller.sync")
	void sync() {
		try {
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
		} catch (Exception e) {
			LOG.error("Keycloak sync failed.", e);
		}
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
		syncAddedGroups(keycloakGroups, databaseGroups, allAuthorities);
		var deletedGroupIds = syncDeletedGroups(keycloakGroups, databaseGroups);
		syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroupIds, allAuthorities);
	}

	//visible for testing
	Map<String, User> syncAddedUsers(Map<String, KeycloakUserDto> keycloakUsers, Map<String, User> databaseUsers) {
		var addedIds = diff(keycloakUsers.keySet(), databaseUsers.keySet());
		var added = addedIds.stream().map(id -> {
			var keycloakUser = keycloakUsers.get(id);
			var databaseUser = new User();
			databaseUser.setId(keycloakUser.id());
			applyUser(databaseUser, keycloakUser);
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
			applyUser(databaseUser, keycloakUser);
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
			var memberIds = keycloakGroup.members().stream().map(KeycloakUserDto::id).collect(Collectors.toSet());
			applyGroup(databaseGroup, keycloakGroup.name(), keycloakGroup.pictureUrl(), memberIds, allAuthorities::get);
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
			var memberIds = keycloakGroup.members().stream().map(KeycloakUserDto::id).collect(Collectors.toSet());
			if (applyGroup(databaseGroup, keycloakGroup.name(), keycloakGroup.pictureUrl(), memberIds, allAuthorities::get)) {
				idsOfGroupsWithChangedMembers.add(id);
			}
		}
		effectiveGroupMembershipRepo.updateGroups(idsOfGroupsWithChangedMembers);
	}

	private void applyUser(User dbUser, KeycloakUserDto keycloakUser) {
		dbUser.setName(keycloakUser.name());
		dbUser.setEmail(keycloakUser.email());
		dbUser.setFirstName(keycloakUser.firstName());
		dbUser.setLastName(keycloakUser.lastName());
		dbUser.setPictureUrl(keycloakUser.pictureUrl());
		dbUser.setEnabled(keycloakUser.enabled());
	}

	private boolean applyGroup(Group dbGroup, String name, String pictureUrl, Set<String> kcMemberIds, Function<String, Authority> memberResolver) {
		dbGroup.setName(name);
		dbGroup.setPictureUrl(pictureUrl);

		var dbMemberIds = dbGroup.getMembers().stream().map(Authority::getId).collect(Collectors.toSet());
		var addedMemberIds = diff(kcMemberIds, dbMemberIds);
		var addedMembers = addedMemberIds.stream().map(memberResolver).filter(Objects::nonNull).collect(Collectors.toSet());
		dbGroup.getMembers().addAll(addedMembers);
		var removedMemberIds = diff(dbMemberIds, kcMemberIds);
		dbGroup.getMembers().removeIf(u -> removedMemberIds.contains(u.getId()));
		return !addedMemberIds.isEmpty() || !removedMemberIds.isEmpty();
	}

	@WithSpan("KeycloakAuthorityPuller.createUser")
	public UserRepresentation createUser(String username, String email, String firstName, String lastName, String password, String pictureUrl, Set<String> groupIds) {
		UserRepresentation user = new UserRepresentation();
		user.setUsername(username);
		user.setEmail(email);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEnabled(true);

		if (pictureUrl != null && !pictureUrl.isBlank()) {
			user.setAttributes(Map.of("picture", List.of(pictureUrl)));
		}

		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(password);
		credential.setTemporary(false);
		user.setCredentials(List.of(credential));

		final String userId;
		try (var response = realm.users().create(user)) {
			userId = switch (response.getStatus()) {
				case 201 -> {
					var location = response.getHeaderString("Location");
					yield location.substring(location.lastIndexOf('/') + 1);
				}
				case 409 -> {
					String body = response.readEntity(String.class);
					ErrorCode errorCode;
					if (body != null && body.contains("same email")) {
						errorCode = ErrorCode.EMAIL_EXISTS;
					} else if (body != null && body.contains("same username")) {
						errorCode = ErrorCode.USERNAME_EXISTS;
					} else {
						errorCode = ErrorCode.USER_EXISTS;
					}
					throw new ErrorCodeException(errorCode);
				}
				default -> {
					LOG.warnv("Failed to create user {0} in Keycloak. Status: {1}", username, response.getStatus());
					throw new ErrorCodeException(ErrorCode.CREATE_USER_FAILED);
				}
			};
		} catch (ProcessingException e) {
			LOG.warnv(e, "Failed to create user {0} in Keycloak.", username);
			throw new ErrorCodeException(ErrorCode.CREATE_USER_FAILED, e);
		}

		UserResource userResource = realm.users().get(userId);

		// groups need to be set after creation, see https://github.com/keycloak/keycloak/discussions/8552
		var joinedGroupIds = new HashSet<String>();
		if (groupIds != null && !groupIds.isEmpty()) {
			for (String groupId : groupIds) {
				try {
					userResource.joinGroup(groupId);
					joinedGroupIds.add(groupId);
				} catch (WebApplicationException e) {
					LOG.warnv(e, "Failed to add user {0} to group {1}.", userId, groupId);
					// TODO: shall we fail the whole user creation here? undo previous steps?
				}
			}
		}

		// sync to db:
		UserRepresentation createdUser = userResource.toRepresentation();
		syncUser(createdUser);

		// mirror the successful group joins into the DB (the user now exists in the DB after syncUser):
		for (String groupId : joinedGroupIds) {
			groupRepo.addMember(groupId, userId);
		}

		// update effective group membership now that the DB contains the membership data
		// (we can assume that the groups already exist, otherwise the caller wouldn't have been able to provide their IDs):
		effectiveGroupMembershipRepo.updateGroups(groupIds);

		return createdUser;
	}

	public UserRepresentation updateUser(String userId, String email, String firstName, String lastName, String password, String pictureUrl) {
		if (isUserReadOnly(userId)) {
			throw new ErrorCodeException(ErrorCode.USER_HAS_FEDERATED_IDENTITY);
		}

		try {
			UserResource userResource = realm.users().get(userId);
			UserRepresentation user = userResource.toRepresentation();

			if (email != null && !email.isBlank()) {
				user.setEmail(email);
			}
			if (firstName != null && !firstName.isBlank()) {
				user.setFirstName(firstName);
			}
			if (lastName != null && !lastName.isBlank()) {
				user.setLastName(lastName);
			}
			var attrs = setPicture(user.getAttributes(), pictureUrl);
			user.setAttributes(attrs);

			userResource.update(user);

			if (password != null && !password.isBlank()) {
				CredentialRepresentation credential = new CredentialRepresentation();
				credential.setType(CredentialRepresentation.PASSWORD);
				credential.setValue(password);
				credential.setTemporary(false);
				userResource.resetPassword(credential);
			}

			syncUser(user);
			return user;
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to update user {0} in Keycloak.", userId);
			switch (keycloakStatus(e)) {
				case 404 -> throw new ErrorCodeException(ErrorCode.USER_NOT_FOUND);
				// the username is immutable, hence a conflict can only be caused by the email address:
				case 409 -> throw new ErrorCodeException(ErrorCode.EMAIL_EXISTS);
				default -> throw new ErrorCodeException(ErrorCode.UPDATE_USER_FAILED, e);
			}
		}
	}

	@Transactional
	public void deleteUser(String userId) {
		if (isUserReadOnly(userId)) {
			throw new ErrorCodeException(ErrorCode.USER_HAS_FEDERATED_IDENTITY);
		}

		// 1. delete from db (roll back if kc deletion fails):
		userRepo.deleteById(userId);

		// 2. delete from kc:
		try (var response = realm.users().delete(userId)) {
			if (response.getStatus() != 204) {
				LOG.warnv("Failed to delete user {0} in Keycloak. Status: {1}", userId, response.getStatus());
				throw new ErrorCodeException(ErrorCode.DELETE_USER_FAILED);
			}
		} catch (ProcessingException e) {
			LOG.warnv(e, "Failed to delete user {0} in Keycloak.", userId);
			throw new ErrorCodeException(ErrorCode.DELETE_USER_FAILED, e);
		}
	}

	@Transactional
	public void setUserEnabled(String userId, boolean enabled) {
		try {
			UserResource userResource = realm.users().get(userId);
			UserRepresentation user = userResource.toRepresentation();
			user.setEnabled(enabled);
			userResource.update(user);
			syncUser(userId);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to {0} user {1} in Keycloak.", enabled ? "enable" : "disable", userId);
			if (keycloakStatus(e) == 404) {
				throw new ErrorCodeException(ErrorCode.USER_NOT_FOUND);
			}
			throw new ErrorCodeException(enabled ? ErrorCode.ENABLE_USER_FAILED : ErrorCode.DISABLE_USER_FAILED, e);
		}
	}

	public boolean isUserReadOnly(String userId) {
		try {
			UserResource userResource = realm.users().get(userId);
			var federatedIdentities = userResource.getFederatedIdentity();
			return !federatedIdentities.isEmpty();
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to check federated identity for user {0}.", userId);
			if (keycloakStatus(e) == 404) {
				throw new ErrorCodeException(ErrorCode.USER_NOT_FOUND);
			}
			throw new ErrorCodeException(ErrorCode.FEDERATED_IDENTITY_CHECK_FAILED, e);
		}
	}

	public User syncUser(String userId) {
		return syncUser(realm.users().get(userId).toRepresentation());
	}

	@Transactional
	@WithSpan("KeycloakAuthorityPuller.syncUser")
	public User syncUser(UserRepresentation userRepresentation) {
		var keycloakUser = KeycloakAuthorityProvider.mapToUser(userRepresentation);

		User dbUser = userRepo.findById(keycloakUser.id());
		if (dbUser == null) {
			dbUser = new User();
			dbUser.setId(keycloakUser.id());
		}
		applyUser(dbUser, keycloakUser);
		userRepo.persist(dbUser);
		return dbUser;
	}

	public Group syncGroup(String groupId) {
		GroupResource groupResource = realm.groups().group(groupId);
		return syncGroup(groupResource, groupResource.toRepresentation());
	}

	@Transactional
	@WithSpan("KeycloakAuthorityPuller.syncGroup")
	public Group syncGroup(GroupResource groupResource, GroupRepresentation keycloakGroup) {
		var groupId = keycloakGroup.getId();
		var memberIds = collectMemberIds(groupResource, groupId);

		Group dbGroup = groupRepo.findById(groupId);
		if (dbGroup == null) {
			dbGroup = new Group();
			dbGroup.setId(groupId);
		}

		var pictureUrl = KeycloakAuthorityProvider.parsePictureUrl(keycloakGroup.getAttributes());
		var dbMembers = userRepo.findByIds(memberIds).collect(Collectors.toMap(User::getId, Function.<User>identity()));

		applyGroup(dbGroup, keycloakGroup.getName(), pictureUrl, new HashSet<>(memberIds), dbMembers::get);
		groupRepo.persist(dbGroup);
		effectiveGroupMembershipRepo.updateGroups(List.of(groupId));
		return dbGroup;
	}

	private List<String> collectMemberIds(GroupResource groupResource, String groupId) {
		List<String> memberIds = new ArrayList<>();
		List<UserRepresentation> currentBatch;
		try {
			do {
				currentBatch = groupResource.members(memberIds.size(), KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST);
				currentBatch.forEach(member -> memberIds.add(member.getId()));
			} while (currentBatch.size() == KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to read members of group {0} from Keycloak (offset {1}).", groupId, memberIds.size());
			throw new ErrorCodeException(ErrorCode.READ_GROUP_MEMBERS_FAILED, e);
		}
		return memberIds;
	}

	@Transactional
	public void addUserToGroup(String groupId, String userId) {
		// 1. sync to db (roll back if kc update fails):
		try {
			groupRepo.addMember(groupId, userId);
			effectiveGroupMembershipRepo.updateGroups(List.of(groupId));
		} catch (PersistenceException e) { // caused by foreign key constraint violation
			throw new ErrorCodeException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
		}

		// 2. sync to kc:
		try {
			realm.users().get(userId).joinGroup(groupId);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to add user {0} to group {1} in Keycloak.", userId, groupId);
			throw new ErrorCodeException(ErrorCode.ADD_GROUP_MEMBER_FAILED, e);
		}
	}

	@Transactional
	public void removeUserFromGroup(String groupId, String userId) {
		// 1. sync to db (roll back if kc update fails):
		groupRepo.removeMember(groupId, userId);
		effectiveGroupMembershipRepo.updateGroups(List.of(groupId));

		// 2. sync to kc:
		try {
			realm.users().get(userId).leaveGroup(groupId);
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to remove user {0} from group {1} in Keycloak.", userId, groupId);
			if (keycloakStatus(e) == 404) {
				throw new ErrorCodeException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
			}
			throw new ErrorCodeException(ErrorCode.REMOVE_GROUP_MEMBER_FAILED, e);
		}
	}

	@Transactional
	@WithSpan("KeycloakAuthorityPuller.updateUserRoles")
	public void updateUserRoles(String userId, Set<RealmRole> roles) {
		// remove roles that are not in the provided set:
		var rolesToRemove = EnumSet.allOf(RealmRole.class);
		rolesToRemove.removeAll(roles);

		// set roles that are in the provided set:
		var rolesToSet = EnumSet.noneOf(RealmRole.class);
		rolesToSet.addAll(roles);

		// 1. sync to db (roll back if kc update fails):
		User dbUser = userRepo.findByIdOptional(userId).orElseThrow(NotFoundException::new);
		dbUser.setRealmRoles(rolesToSet.stream().map(RealmRole::kcName).toArray(String[]::new));
		userRepo.persist(dbUser);

		// 2. sync to kc:
		try {
			UserResource userResource = realm.users().get(userId);
			var roleMappings = userResource.roles().realmLevel();
			if (!rolesToRemove.isEmpty()) {
				roleMappings.remove(rolesToRemove.stream().map(realmRoles::getRealmRole).toList());
			}
			if (!rolesToSet.isEmpty()) {
				roleMappings.add(rolesToSet.stream().map(realmRoles::getRealmRole).toList());
			}
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to update realm roles for user {0} in Keycloak.", userId);
			throw new ErrorCodeException(ErrorCode.UPDATE_USER_ROLES_FAILED, e);
		}
	}

	// Group management methods

	public GroupRepresentation createGroup(String name, String pictureUrl) {
		GroupRepresentation group = new GroupRepresentation();
		group.setName(name);

		if (pictureUrl != null && !pictureUrl.isBlank()) {
			group.setAttributes(Map.of("picture", List.of(pictureUrl)));
		}

		final String groupId;
		try (var response = realm.groups().add(group)) {
			groupId = switch (response.getStatus()) {
				case 201 -> {
					var location = response.getHeaderString("Location");
					yield location.substring(location.lastIndexOf('/') + 1);
				}
				case 409 -> throw new ErrorCodeException(ErrorCode.GROUP_NAME_EXISTS);
				default -> {
					LOG.warnv("Failed to create group {0} in Keycloak. Status: {1}", name, response.getStatus());
					throw new ErrorCodeException(ErrorCode.CREATE_GROUP_FAILED);
				}
			};
		} catch (ProcessingException e) {
			LOG.warnv(e, "Failed to create group {0} in Keycloak.", name);
			throw new ErrorCodeException(ErrorCode.CREATE_GROUP_FAILED, e);
		}

		GroupResource groupResource = realm.groups().group(groupId);
		GroupRepresentation createdGroup = groupResource.toRepresentation();
		syncGroup(groupResource, createdGroup);
		return createdGroup;
	}

	public GroupRepresentation updateGroup(String groupId, String name, String pictureUrl) {
		try {
			GroupResource groupResource = realm.groups().group(groupId);
			GroupRepresentation group = groupResource.toRepresentation();

			if (!name.isBlank()) {
				group.setName(name);
			}

			var attrs = setPicture(group.getAttributes(), pictureUrl);
			group.setAttributes(attrs);

			groupResource.update(group);
			syncGroup(groupResource, group);
			return group;
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to update group {0} in Keycloak.", groupId);
			switch (keycloakStatus(e)) {
				case 404 -> throw new ErrorCodeException(ErrorCode.GROUP_NOT_FOUND);
				case 409 -> throw new ErrorCodeException(ErrorCode.GROUP_NAME_EXISTS);
				default -> throw new ErrorCodeException(ErrorCode.UPDATE_GROUP_FAILED, e);
			}
		}
	}

	@Transactional
	public void deleteGroup(String groupId) {
		// 1. delete from db (roll back if kc deletion fails):
		groupRepo.deleteById(groupId);

		// 2. delete from kc:
		try {
			realm.groups().group(groupId).remove();
		} catch (WebApplicationException | ProcessingException e) {
			LOG.warnv(e, "Failed to delete group {0} in Keycloak.", groupId);
			if (keycloakStatus(e) == 404) {
				throw new ErrorCodeException(ErrorCode.GROUP_NOT_FOUND);
			}
			throw new ErrorCodeException(ErrorCode.DELETE_GROUP_FAILED, e);
		}
	}

	private static int keycloakStatus(Exception e) {
		return e instanceof WebApplicationException wae ? wae.getResponse().getStatus() : -1;
	}

	private static Map<String, List<String>> setPicture(Map<String, List<String>> attributes, String pictureUrl) {
		Map<String, List<String>> attrs = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
		if (pictureUrl == null || pictureUrl.isBlank()) {
			attrs.remove("picture");
		} else {
			attrs.put("picture", List.of(pictureUrl));
		}
		return attrs;
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
