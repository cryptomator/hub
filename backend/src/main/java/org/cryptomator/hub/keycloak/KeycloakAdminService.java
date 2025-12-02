package org.cryptomator.hub.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class KeycloakAdminService {

	private static final Logger LOG = LoggerFactory.getLogger(KeycloakAdminService.class);

	@Inject
	Keycloak keycloak;

	@Inject
	KeycloakAuthorityProvider authorityProvider;

	@Inject
	User.Repository userRepo;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	public UserRepresentation createUser(String username, String email, String firstName, String lastName, String password, String pictureUrl, Set<String> groupIds) {
		RealmResource realm = keycloak.realm(keycloakRealm);

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

		var response = realm.users().create(user);
		if (response.getStatus() != 201) {
			throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
		}

		String locationHeader = response.getHeaderString("Location");
		String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

		UserResource userResource = realm.users().get(userId);

		if (pictureUrl != null && !pictureUrl.isBlank()) {
			UserRepresentation createdUser = userResource.toRepresentation();
			createdUser.setAttributes(Map.of("picture", List.of(pictureUrl)));
			userResource.update(createdUser);
		}

		if (groupIds != null && !groupIds.isEmpty()) {
			for (String groupId : groupIds) {
				try {
					userResource.joinGroup(groupId);
				} catch (Exception e) {
					LOG.warn("Failed to add user {} to group {}", userId, groupId, e);
				}
			}
		}

		syncUser(userId);

		return realm.users().get(userId).toRepresentation();
	}

	public UserRepresentation getUser(String userId) {
		RealmResource realm = keycloak.realm(keycloakRealm);
		try {
			return realm.users().get(userId).toRepresentation();
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new NotFoundException("User not found: " + userId);
		}
	}

	public UserRepresentation updateUser(String userId, String firstName, String lastName, String password, String pictureUrl) {
		if (isUserReadOnly(userId)) {
			throw new ForbiddenException("User has a federated identity and cannot be modified");
		}

		RealmResource realm = keycloak.realm(keycloakRealm);
		UserResource userResource;
		try {
			userResource = realm.users().get(userId);
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new NotFoundException("User not found: " + userId);
		}

		UserRepresentation user = userResource.toRepresentation();

		if (firstName != null && !firstName.isBlank()) {
			user.setFirstName(firstName);
		}
		if (lastName != null && !lastName.isBlank()) {
			user.setLastName(lastName);
		}
		if (pictureUrl != null) {
			if (pictureUrl.isBlank()) {
				user.setAttributes(Collections.emptyMap());
			} else {
				user.setAttributes(Map.of("picture", List.of(pictureUrl)));
			}
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

		RealmResource realm = keycloak.realm(keycloakRealm);
		try {
			realm.users().delete(userId);
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new NotFoundException("User not found: " + userId);
		}

		User user = userRepo.findById(userId);
		if (user != null) {
			userRepo.delete(user);
		}
	}

	public boolean isUserReadOnly(String userId) {
		RealmResource realm = keycloak.realm(keycloakRealm);
		try {
			UserResource userResource = realm.users().get(userId);
			var federatedIdentities = userResource.getFederatedIdentity();
			return !federatedIdentities.isEmpty();
		} catch (Exception e) {
			LOG.warn("Failed to check federated identity for user {}", userId, e);
			return false;
		}
	}

	public User syncUser(String userId) {
		try {
			RealmResource realm = keycloak.realm(keycloakRealm);
			UserResource userResource = realm.users().get(userId);
			UserRepresentation keycloakUser = userResource.toRepresentation();

			User dbUser = userRepo.findById(userId);
			if (dbUser == null) {
				dbUser = new User();
				dbUser.setId(keycloakUser.getId());
			}

			dbUser.setName(keycloakUser.getUsername());
			dbUser.setEmail(keycloakUser.getEmail());

			String pictureUrl = null;
			if (keycloakUser.getAttributes() != null) {
				var pictureAttr = keycloakUser.getAttributes().get("picture");
				if (pictureAttr != null && !pictureAttr.isEmpty()) {
					pictureUrl = pictureAttr.get(0);
				}
			}
			dbUser.setPictureUrl(pictureUrl);

			userRepo.persist(dbUser);
			userRepo.flush();
			return dbUser;
		} catch (jakarta.ws.rs.NotFoundException e) {
			throw new NotFoundException("User not found in Keycloak: " + userId);
		} catch (Exception e) {
			LOG.error("Failed to sync user {}", userId, e);
			throw new RuntimeException("Failed to sync user: " + userId, e);
		}
	}
}
