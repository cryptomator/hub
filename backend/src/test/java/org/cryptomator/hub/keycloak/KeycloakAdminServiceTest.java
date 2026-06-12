package org.cryptomator.hub.keycloak;

import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class KeycloakAdminServiceTest {

	private final RealmResource realm = Mockito.mock(RealmResource.class);
	private final User.Repository userRepo = Mockito.mock(User.Repository.class);
	private final Group.Repository groupRepo = Mockito.mock(Group.Repository.class);
	private final EffectiveGroupMembership.Repository effectiveGroupMembershipRepo = Mockito.mock(EffectiveGroupMembership.Repository.class);
	private final KeycloakRealmRoles realmRoles = Mockito.mock(KeycloakRealmRoles.class);

	private final UserResource userResource = Mockito.mock(UserResource.class);
	private final RoleMappingResource roleMappings = Mockito.mock(RoleMappingResource.class);
	private final RoleScopeResource realmLevel = Mockito.mock(RoleScopeResource.class);

	// the exact RoleRepresentation instances Keycloak hands back per role, so verify(...) matches by reference:
	private final EnumMap<RealmRole, RoleRepresentation> roleReps = new EnumMap<>(RealmRole.class);

	private KeycloakAdminService service;

	@BeforeEach
	void setUp() {
		service = new KeycloakAdminService();
		service.realm = realm;
		service.userRepo = userRepo;
		service.groupRepo = groupRepo;
		service.effectiveGroupMembershipRepo = effectiveGroupMembershipRepo;
		service.realmRoles = realmRoles;

		var usersResource = Mockito.mock(UsersResource.class);
		Mockito.when(realm.users()).thenReturn(usersResource);
		Mockito.when(usersResource.get("U_test")).thenReturn(userResource);
		Mockito.when(userResource.roles()).thenReturn(roleMappings);
		Mockito.when(roleMappings.realmLevel()).thenReturn(realmLevel);

		for (var role : RealmRole.values()) {
			var rep = role(role.kcName());
			roleReps.put(role, rep);
			Mockito.when(realmRoles.getRealmRole(role)).thenReturn(rep);
		}
	}

	@Nested
	@DisplayName("updateUserRoles")
	class UpdateUserRoles {

		@Test
		@DisplayName("assigns the requested roles, removes the rest, and reads the effective roles back into the db")
		void testUpdateUserRoles() {
			var dbUser = new User();
			dbUser.setId("U_test");
			Mockito.when(userRepo.findByIdOptional("U_test")).thenReturn(Optional.of(dbUser));
			// Keycloak reports the assigned roles back after the update:
			Mockito.when(realmLevel.listAll()).thenReturn(List.of(roleReps.get(RealmRole.USER), roleReps.get(RealmRole.ADMIN)));

			service.updateUserRoles("U_test", Set.of(RealmRole.USER, RealmRole.ADMIN));

			Mockito.verify(userRepo).persist(dbUser);
			// CREATE_VAULTS is the only role not in the requested set:
			Mockito.verify(realmLevel).remove(List.of(roleReps.get(RealmRole.CREATE_VAULTS)));
			Mockito.verify(realmLevel).add(List.of(roleReps.get(RealmRole.USER), roleReps.get(RealmRole.ADMIN)));
			// final db state mirrors what Keycloak reported, not the requested set:
			Assertions.assertArrayEquals(new String[]{"user", "admin"}, dbUser.getRealmRoles());
		}

		@Test
		@DisplayName("removes all roles when given an empty set")
		void testClearUserRoles() {
			var dbUser = new User();
			dbUser.setId("U_test");
			Mockito.when(userRepo.findByIdOptional("U_test")).thenReturn(Optional.of(dbUser));
			Mockito.when(realmLevel.listAll()).thenReturn(List.of());

			service.updateUserRoles("U_test", Set.of());

			Mockito.verify(realmLevel).remove(List.of(roleReps.get(RealmRole.USER), roleReps.get(RealmRole.ADMIN), roleReps.get(RealmRole.CREATE_VAULTS)));
			Mockito.verify(realmLevel, Mockito.never()).add(Mockito.anyList());
			Assertions.assertArrayEquals(new String[0], dbUser.getRealmRoles());
		}

		@Test
		@DisplayName("throws and leaves Keycloak untouched when the user is unknown to the db")
		void testUpdateRolesUnknownUser() {
			Mockito.when(userRepo.findByIdOptional("U_test")).thenReturn(Optional.empty());

			Assertions.assertThrows(NotFoundException.class, () -> service.updateUserRoles("U_test", Set.of(RealmRole.USER)));

			Mockito.verifyNoInteractions(realmLevel);
		}
	}

	@Nested
	@DisplayName("syncUser")
	class SyncUser {

		@Test
		@DisplayName("syncs known realm roles from Keycloak and ignores unknown ones")
		void testSyncRealmRoles() {
			var keycloakUser = new UserRepresentation();
			keycloakUser.setId("U_test");
			keycloakUser.setUsername("alice");
			keycloakUser.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(keycloakUser);
			Mockito.when(userRepo.findById("U_test")).thenReturn(null);
			// "offline_access" is not a hub RealmRole and must be dropped:
			Mockito.when(realmLevel.listAll()).thenReturn(List.of(role("user"), role("admin"), role("offline_access")));

			var dbUser = service.syncUser("U_test");

			Assertions.assertArrayEquals(new String[]{"user", "admin"}, dbUser.getRealmRoles());
			Mockito.verify(userRepo).persist(dbUser);
		}
	}

	private static RoleRepresentation role(String name) {
		var rep = new RoleRepresentation();
		rep.setName(name);
		return rep;
	}
}
