package org.cryptomator.hub.keycloak;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class KeycloakAdminService {

	private static final Logger LOG = LoggerFactory.getLogger(KeycloakAdminService.class);

	@Inject
	Keycloak keycloak;

	@Inject
	User.Repository userRepo;

	@Inject
	Group.Repository groupRepo;

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
					String errorMessage;
					if (body != null && body.contains("same email")) {
						errorMessage = "EMAIL_EXISTS";
					} else if (body != null && body.contains("same username")) {
						errorMessage = "USERNAME_EXISTS";
					} else {
						errorMessage = "User already exists";
					}
					throw new ClientErrorException(errorMessage, Response.Status.CONFLICT);
				}
				default -> throw new InternalServerErrorException("Failed to create user in Keycloak. Status: " + response.getStatus());
			};
		}

		UserResource userResource = realm.users().get(userId);

		// groups need to be set after creation, see https://github.com/keycloak/keycloak/discussions/8552
		if (groupIds != null && !groupIds.isEmpty()) {
			for (String groupId : groupIds) {
				try {
					userResource.joinGroup(groupId);
				} catch (WebApplicationException e) {
					LOG.warn("Failed to add user {} to group {}", userId, groupId, e);
					// TODO: shall we fail the whole user creation here? undo previous steps?
				}
			}
			effectiveGroupMembershipRepo.updateGroups(groupIds);
		}

		syncUser(userId);

		return realm.users().get(userId).toRepresentation();
	}

	public UserRepresentation updateUser(String userId, String email, String firstName, String lastName, String password, String pictureUrl) {
		if (isUserReadOnly(userId)) {
			throw new ForbiddenException("User has a federated identity and cannot be modified");
		}

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
		if (pictureUrl != null) {
			var attrs = setPicture(user.getAttributes(), pictureUrl);
			user.setAttributes(attrs);
		}

		userResource.update(user);

		if (password != null && !password.isBlank()) {
			CredentialRepresentation credential = new CredentialRepresentation();
			credential.setType(CredentialRepresentation.PASSWORD);
			credential.setValue(password);
			credential.setTemporary(false);
			userResource.resetPassword(credential);
		}

		syncUser(userId);
		return userResource.toRepresentation();
	}

	@Transactional
	public void deleteUser(String userId) {
		if (isUserReadOnly(userId)) {
			throw new ForbiddenException("User has a federated identity and cannot be deleted");
		}

		try (var response = realm.users().delete(userId)){
			if (response.getStatus() != 204) {
				throw new InternalServerErrorException("Failed to delete user in Keycloak. Status: " + response.getStatus());
			}
		}

		userRepo.deleteById(userId);
	}

	public boolean isUserReadOnly(String userId) {
		try {
			UserResource userResource = realm.users().get(userId);
			var federatedIdentities = userResource.getFederatedIdentity();
			return !federatedIdentities.isEmpty();
		} catch (WebApplicationException e) {
			LOG.warn("Failed to check federated identity for user {}. Keycloak responded with status {}", userId, e.getResponse().getStatus(), e);
			throw new InternalServerErrorException("Failed to check federated identity", e);
		}
	}

	// TODO deduplicate with KeycloakAuthorityPuller
	@Transactional
	public User syncUser(String userId) {
		UserResource userResource = realm.users().get(userId);
		UserRepresentation keycloakUser = userResource.toRepresentation();

		User dbUser = userRepo.findById(userId);
		if (dbUser == null) {
			dbUser = new User();
			dbUser.setId(keycloakUser.getId());
		}

		dbUser.setName(keycloakUser.getUsername());
		dbUser.setEmail(keycloakUser.getEmail());
		dbUser.setFirstName(keycloakUser.getFirstName());
		dbUser.setLastName(keycloakUser.getLastName());

		var attrs = keycloakUser.getAttributes();
		if (attrs != null && attrs.containsKey("picture")) {
			var pictureAttr = attrs.get("picture");
			dbUser.setPictureUrl(pictureAttr.isEmpty() ? null : pictureAttr.getFirst());
		} else {
			dbUser.setPictureUrl(null);
		}

		userRepo.persist(dbUser);
		return dbUser;
	}

	// TODO deduplicate with KeycloakAuthorityPuller
	@Transactional
	public Group syncGroup(String groupId) {
		GroupResource groupResource = realm.groups().group(groupId);
		GroupRepresentation keycloakGroup = groupResource.toRepresentation();

		Group dbGroup = groupRepo.findById(groupId);
		if (dbGroup == null) {
			dbGroup = new Group();
			dbGroup.setId(keycloakGroup.getId());
		}

		dbGroup.setName(keycloakGroup.getName());

		var attrs = keycloakGroup.getAttributes();
		if (attrs != null && attrs.containsKey("picture")) {
			var pictureAttr = attrs.get("picture");
			dbGroup.setPictureUrl(pictureAttr.isEmpty() ? null : pictureAttr.getFirst());
		} else {
			dbGroup.setPictureUrl(null);
		}

		// Sync members
		var keycloakMembers = groupResource.members().stream().map(UserRepresentation::getId).toList();
		var dbMembers = userRepo.findByIds(keycloakMembers).toList();
		dbGroup.getMembers().clear();
		dbGroup.getMembers().addAll(dbMembers);
		groupRepo.persist(dbGroup);
		effectiveGroupMembershipRepo.updateGroups(Set.of(dbGroup.getId()));
		return dbGroup;
	}

	@Transactional
	public void addUserToGroup(String groupId, String userId) {
		realm.groups().group(groupId).toRepresentation(); // trigger NotFoundException if group does not exist
		UserResource userResource = realm.users().get(userId);
		userResource.joinGroup(groupId);

		syncGroup(groupId);
	}

	@Transactional
	public void removeUserFromGroup(String groupId, String userId) {
		realm.groups().group(groupId).toRepresentation(); // trigger NotFoundException if group does not exist
		UserResource userResource = realm.users().get(userId);
		userResource.leaveGroup(groupId);

		syncGroup(groupId);
	}

	public void updateUserRoles(String userId, Set<RealmRole> roles) {
		UserResource userResource = realm.users().get(userId);
		var roleMappings = userResource.roles().realmLevel();

		// remove roles that are not in the provided set:
		var rolesToRemove = EnumSet.allOf(RealmRole.class);
		rolesToRemove.removeAll(roles);
		if (!rolesToRemove.isEmpty()) {
			roleMappings.remove(rolesToRemove.stream().map(realmRoles::getRealmRole).toList());
		}

		// set roles that are in the provided set:
		var rolesToSet = EnumSet.noneOf(RealmRole.class);
		rolesToSet.addAll(roles);
		if (!rolesToSet.isEmpty()) {
			roleMappings.add(rolesToSet.stream().map(realmRoles::getRealmRole).toList());
		}

		// sync to db:
		User dbUser = userRepo.findById(userId);
		if (dbUser != null) {
			dbUser.setRealmRoles(rolesToSet.stream().map(RealmRole::kcName).toArray(String[]::new));
			userRepo.persist(dbUser);
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
				case 409 -> throw new ClientErrorException("GROUP_NAME_EXISTS", Response.Status.CONFLICT);
				default -> throw new InternalServerErrorException("Failed to create group in Keycloak. Status: " + response.getStatus());
			};
		}

		syncGroup(groupId);
		return realm.groups().group(groupId).toRepresentation();
	}

	public GroupRepresentation updateGroup(String groupId, String name, String pictureUrl) {
		GroupResource groupResource = realm.groups().group(groupId);
		GroupRepresentation group = groupResource.toRepresentation();

		if (!name.isBlank()) {
			group.setName(name);
		}

		if (pictureUrl != null) {
			var attrs = setPicture(group.getAttributes(), pictureUrl);
			group.setAttributes(attrs);
		}

		groupResource.update(group);
		syncGroup(groupId);
		return groupResource.toRepresentation();
	}

	@Transactional
	public void deleteGroup(String groupId) {
		realm.groups().group(groupId).remove();
		groupRepo.deleteById(groupId);
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

}

