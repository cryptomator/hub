package org.cryptomator.hub.keycloak;

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
import org.mockito.Mockito;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;

class KeycloakAdminServiceTest {

	private final RealmResource realm = Mockito.mock(RealmResource.class);
	private final KeycloakRealmRoles realmRoles = Mockito.mock(KeycloakRealmRoles.class);

	private final UserResource userResource = Mockito.mock(UserResource.class);
	private final RoleMappingResource roleMappings = Mockito.mock(RoleMappingResource.class);
	private final RoleScopeResource realmLevel = Mockito.mock(RoleScopeResource.class);

	// the exact RoleRepresentation instances Keycloak hands back per role, so verify(...) matches by reference:
	private final EnumMap<RealmRole, RoleRepresentation> roleReps = new EnumMap<>(RealmRole.class);

	private KeycloakAdminService service;

	@BeforeEach
	void setUp() {
		service = new KeycloakAdminService(null, null, null, null, realmRoles, null);
		service.realm = realm;

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
		@DisplayName("assigns the requested roles and removes the rest in Keycloak")
		void testUpdateUserRoles() {
			service.updateUserRoles("U_test", Set.of(RealmRole.USER, RealmRole.ADMIN));

			// CREATE_VAULTS is the only role not in the requested set:
			Mockito.verify(realmLevel).remove(List.of(roleReps.get(RealmRole.CREATE_VAULTS)));
			Mockito.verify(realmLevel).add(List.of(roleReps.get(RealmRole.USER), roleReps.get(RealmRole.ADMIN)));
		}

		@Test
		@DisplayName("removes all roles when given an empty set")
		void testClearUserRoles() {
			service.updateUserRoles("U_test", Set.of());

			Mockito.verify(realmLevel).remove(List.of(roleReps.get(RealmRole.USER), roleReps.get(RealmRole.ADMIN), roleReps.get(RealmRole.CREATE_VAULTS)));
			Mockito.verify(realmLevel, Mockito.never()).add(Mockito.anyList());
		}
	}

	@Nested
	@DisplayName("realmRolesOf")
	class RealmRolesOf {

		@Test
		@DisplayName("returns known realm roles from Keycloak and ignores unknown ones")
		void testReadKnownRoles() {
			// "offline_access" is not a hub RealmRole and must be dropped:
			Mockito.when(realmLevel.listAll()).thenReturn(List.of(role("user"), role("admin"), role("offline_access")));

			var result = service.realmRolesOf("U_test");

			Assertions.assertEquals(Set.of("user", "admin"), result);
		}
	}

	private static RoleRepresentation role(String name) {
		var rep = new RoleRepresentation();
		rep.setName(name);
		return rep;
	}
}
