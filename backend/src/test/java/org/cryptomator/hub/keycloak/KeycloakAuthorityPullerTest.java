package org.cryptomator.hub.keycloak;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

class KeycloakAuthorityPullerTest {

	private final KeycloakAuthorityProvider remoteUserProvider = Mockito.mock(KeycloakAuthorityProvider.class);
	private final User.Repository userRepo = Mockito.mock(User.Repository.class);
	private final Group.Repository groupRepo = Mockito.mock(Group.Repository.class);
	private final EffectiveGroupMembership.Repository effectiveGroupMembershipRepo = Mockito.mock(EffectiveGroupMembership.Repository.class);

	private final List<User> persistedUsers = new ArrayList<>();
	private final List<Group> persistedGroups = new ArrayList<>();

	private KeycloakAuthorityPuller remoteUserPuller;

	@BeforeEach
	void setUp() {
		remoteUserPuller = new KeycloakAuthorityPuller();
		remoteUserPuller.remoteUserProvider = remoteUserProvider;
		remoteUserPuller.userRepo = userRepo;
		remoteUserPuller.groupRepo = groupRepo;
		remoteUserPuller.effectiveGroupMembershipRepo = effectiveGroupMembershipRepo;
		persistedUsers.clear();
		Mockito.doAnswer(invocation -> {
			Iterable<User> iterable = invocation.getArgument(0);
			iterable.forEach(persistedUsers::add);
			return null;
		}).when(userRepo).persist(Mockito.<Iterable<User>>any());
		persistedGroups.clear();
		Mockito.doAnswer(invocation -> {
			Iterable<Group> iterable = invocation.getArgument(0);
			iterable.forEach(persistedGroups::add);
			return null;
		}).when(groupRepo).persist(Mockito.<Iterable<Group>>any());
	}

	@Nested
	@DisplayName("Test add/delete Users")
	class AddDeleteUsers {

		@DisplayName("test add users")
		@ParameterizedTest(name = "KCUser: {0} DBUser: {1} AddedUser: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;foo,bar,baz",
				"foo,bar,baz;la,bar,baz;foo",
				"baz;foo,bar,baz;,",
				",;foo,bar,baz;,",
				",;,;,"
		}, delimiterString = ";")
		void testAddUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] addedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock();
			Map<String, User> databaseUsers = Mockito.mock();

			var keycloakUserIds = Set.of(keycloakUserIdString);
			var databaseUserIds = Set.of(databaseUserIdString);
			var addedUserIds = Set.of(addedUserIdString);

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (var userId : addedUserIds) {
				var keycloakUser = new KeycloakUserDto(userId, "name " + userId, "email " + userId, "first " + userId, "last " + userId, "pic " + userId, true, Set.of());
				Mockito.when(keycloakUsers.get(userId)).thenReturn(keycloakUser);
			}

			var added = remoteUserPuller.syncAddedUsers(keycloakUsers, databaseUsers);

			Assertions.assertEquals(addedUserIds, added.keySet());
			Mockito.verify(userRepo).persist(Mockito.<Iterable<User>>any());
			Mockito.verify(effectiveGroupMembershipRepo).updateUsers(Mockito.argThat(addedUserIds::containsAll));
			for (var userId : addedUserIds) {
				MatcherAssert.assertThat(persistedUsers, Matchers.hasItem(
						Matchers.allOf(
								Matchers.hasProperty("id", Matchers.equalTo(userId)),
								Matchers.hasProperty("name", Matchers.equalTo("name " + userId)),
								Matchers.hasProperty("email", Matchers.equalTo("email " + userId)),
								Matchers.hasProperty("firstName", Matchers.equalTo("first " + userId)),
								Matchers.hasProperty("lastName", Matchers.equalTo("last " + userId)),
								Matchers.hasProperty("pictureUrl", Matchers.equalTo("pic " + userId)),
								Matchers.hasProperty("enabled", Matchers.equalTo(true))
						)
				));
			}
		}

		@DisplayName("test delete users")
		@ParameterizedTest(name = "KCUser: {0} DBUser: {1} DeletedUser: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;,",
				"foo,bar,baz;la,li,lu;la,li,lu",
				"foo,bar,baz;la,bar,baz;la",
				"baz;foo,bar,baz;foo,bar",
				",;foo,bar,baz;foo,bar,baz",
				",;,;,"
		}, delimiterString = ";")
		void testDeleteUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock();
			Map<String, User> databaseUsers = Mockito.mock();

			var keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			var databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			var deletedMap = Arrays.stream(deletedUserIdString).collect(Collectors.toMap(Function.identity(), id -> Mockito.mock(User.class)));

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			deletedMap.forEach((id, user) ->
					Mockito.when(databaseUsers.get(id)).thenReturn(user));

			var result = remoteUserPuller.syncDeletedUsers(keycloakUsers, databaseUsers);

			var expected = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());
			MatcherAssert.assertThat(result, Matchers.equalTo(expected));
			Mockito.verify(userRepo).deleteByIds(expected);
			Mockito.verify(effectiveGroupMembershipRepo).updateUsers(Mockito.argThat(expected::containsAll));
		}
	}

	@Nested
	@DisplayName("Test update Users")
	class UpdateUsers {

		@DisplayName("test update users")
		@ParameterizedTest(name = "KCUser: {0} DBUser: {1} Deleted: {2} Updated: {3}")
		@CsvSource(value = {
				"foo,bar,baz;foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;la,li,lu;,", // foo,bar,baz new, la,li,lu removed
				",;foo,bar,baz;foo,bar,baz;,", // all removed
				"foo,bar,baz;,;,;,", // all new
				",;,;,;," // all empty
		}, delimiterString = ";")
		void testUpdateUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString, @ConvertWith(StringArrayConverter.class) String[] updatedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock();
			Map<String, User> databaseUsers = Mockito.mock();

			var keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			var databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			var deletedUserIds = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());
			var updatedUserIds = Arrays.stream(updatedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (var userId : updatedUserIds) {
				var keycloakUser = new KeycloakUserDto(userId, "name " + userId, "email " + userId, "first " + userId, "last " + userId, "pic " + userId, true, Set.of());
				Mockito.when(keycloakUsers.get(userId)).thenReturn(keycloakUser);

				var databaseUser = Mockito.mock(User.class);
				Mockito.when(databaseUser.getId()).thenReturn(userId);
				Mockito.when(databaseUsers.get(userId)).thenReturn(databaseUser);
			}

			remoteUserPuller.syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUserIds);

			for (var userId : updatedUserIdString) {
				var databaseUser = databaseUsers.get(userId);
				Mockito.verify(databaseUser).setName("name " + userId);
				Mockito.verify(databaseUser).setEmail("email " + userId);
				Mockito.verify(databaseUser).setFirstName("first " + userId);
				Mockito.verify(databaseUser).setLastName("last " + userId);
				Mockito.verify(databaseUser).setPictureUrl("pic " + userId);
				Mockito.verify(databaseUser).setEnabled(true);
			}
			Mockito.verify(userRepo, Mockito.never()).persist(any(User.class));
		}
	}

	@Nested
	@DisplayName("Test add/delete Groups")
	class AddDeleteGroups {

		@DisplayName("test add groups")
		@ParameterizedTest(name = "KCGroup: {0} DBGroup: {1} AddedGroup: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;foo,bar,baz",
				"foo,bar,baz;la,bar,baz;foo",
				"baz;foo,bar,baz;,",
				",;foo,bar,baz;,",
				",;,;,"
		}, delimiterString = ";")
		void testAddGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] addedGroupIdString) {
			Map<String, KeycloakUserDto> kcUsers = new HashMap<>();
			for (var gid : keycloakGroupIdString) {
				kcUsers.put(gid, new KeycloakUserDto(gid, "username", "email", "first", "last", "pic", true, Set.of()));
			}

			Map<String, KeycloakGroupDto> keycloakGroups = new HashMap<>();
			for (var gid : keycloakGroupIdString) {
				var dto = new KeycloakGroupDto(gid, "name " + gid, "pic " + gid, Set.of(kcUsers.get(gid)));
				keycloakGroups.put(gid, dto);
			}

			Map<String, Authority> databaseUsers = new HashMap<>();
			for (var keycloakUser : kcUsers.values()) {
				var databaseUser = Mockito.mock(User.class);
				Mockito.when(databaseUser.getId()).thenReturn(keycloakUser.id());
				databaseUsers.put(keycloakUser.id(), databaseUser);
			}

			Map<String, Group> databaseGroups = new HashMap<>();
			for (var gid : databaseGroupIdString) {
				var databaseGroup = Mockito.mock(Group.class);
				Mockito.when(databaseGroup.getId()).thenReturn(gid);
				databaseGroups.put(gid, databaseGroup);
			}

			var added = remoteUserPuller.syncAddedGroups(keycloakGroups, databaseGroups, databaseUsers);

			var addedGroupIds = Set.of(addedGroupIdString);
			Assertions.assertEquals(addedGroupIds, added.keySet());
			Mockito.verify(groupRepo).persist(Mockito.<Iterable<Group>>any());
			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(Mockito.argThat(addedGroupIds::containsAll));
			for (var groupId : addedGroupIds) {
				MatcherAssert.assertThat(persistedGroups, Matchers.hasItem(
						Matchers.allOf(
								Matchers.hasProperty("id", Matchers.equalTo(groupId)),
								Matchers.hasProperty("pictureUrl", Matchers.equalTo("pic " + groupId)),
								Matchers.hasProperty("name", Matchers.equalTo("name " + groupId)),
								Matchers.hasProperty("members", Matchers.contains(Matchers.hasProperty("id", Matchers.equalTo(groupId))))
						)
				));
			}
		}

		@DisplayName("test delete groups")
		@ParameterizedTest(name = "KCGroup: {0} DBGroup: {1} DeletedGroup: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;,",
				"foo,bar,baz;la,li,lu;la,li,lu",
				"foo,bar,baz;la,bar,baz;la",
				"baz;foo,bar,baz;foo,bar",
				",;foo,bar,baz;foo,bar,baz",
				",;,;,"
		}, delimiterString = ";")
		void testDeleteGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString) {
			Map<String, KeycloakGroupDto> keycloakGroups = Mockito.mock();
			Map<String, Group> databaseGroups = Mockito.mock();

			var kcGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
			var dbGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
			var deletedMap = Arrays.stream(deletedGroupIdString).collect(Collectors.toMap(Function.identity(), id -> Mockito.mock(Group.class)));

			Mockito.when(keycloakGroups.keySet()).thenReturn(kcGroupIds);
			Mockito.when(databaseGroups.keySet()).thenReturn(dbGroupIds);

			deletedMap.forEach((id, group) -> Mockito.when(databaseGroups.get(id)).thenReturn(group));

			var result = remoteUserPuller.syncDeletedGroups(keycloakGroups, databaseGroups);

			var expected = Arrays.stream(deletedGroupIdString).collect(Collectors.toSet());
			MatcherAssert.assertThat(result, Matchers.equalTo(expected));
			Mockito.verify(groupRepo).deleteByIds(expected);
			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(Mockito.argThat(expected::containsAll));
		}
	}

	@DisplayName("test update groups")
	@ParameterizedTest(name = "KCGroups: {0} DBGroups: {1} DeletedGroups: {2} UpdatedGroups: {3} ")
	@CsvSource(value = {
			"foo,bar,baz;foo,bar,baz;,;foo,bar,baz",
			"foo,bar,baz;la,li,lu;la,li,lu;,",
			",;foo,bar,baz;foo,bar,baz;,",
			"foo,bar,baz;,;,;,",
			",;,;,;,"
	}, delimiterString = ";")
	void testUpdateGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString, @ConvertWith(StringArrayConverter.class) String[] updatedGroupIdString) {
		var dbOnlyUser = Mockito.mock(User.class);
		Mockito.when(dbOnlyUser.getId()).thenReturn("U_dbOnly");
		var otherKCUser = Mockito.mock(User.class);
		Mockito.when(otherKCUser.getId()).thenReturn("U_otherKC");

		Map<String, KeycloakGroupDto> keycloakGroups = Mockito.mock();
		Map<String, Group> databaseGroups = Mockito.mock();

		var keycloakGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
		var databaseGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
		var updatedGroupIds = Arrays.stream(updatedGroupIdString).collect(Collectors.toSet());

		Mockito.when(keycloakGroups.keySet()).thenReturn(keycloakGroupIds);
		Mockito.when(databaseGroups.keySet()).thenReturn(databaseGroupIds);

		var dbGroupMembers = new HashSet<Authority>(Set.of(dbOnlyUser));
		for (var groupId : updatedGroupIds) {
			var kcDto = Mockito.mock(KeycloakGroupDto.class);
			Mockito.when(kcDto.name()).thenReturn(String.format("name %s", groupId));
			Mockito.when(kcDto.pictureUrl()).thenReturn(String.format("pic %s", groupId));
			Mockito.when(kcDto.members()).thenReturn(Set.of(
					new KeycloakUserDto("U_user", "n", "e", "f", "l", "p", true, Set.of()),
					new KeycloakUserDto("U_otherKC", "n", "e", "f", "l", "p", true, Set.of())
			));

			var dbGroup = Mockito.mock(Group.class);
			Mockito.when(dbGroup.getId()).thenReturn(groupId);
			Mockito.when(dbGroup.getMembers()).thenReturn(dbGroupMembers);

			Mockito.when(keycloakGroups.get(groupId)).thenReturn(kcDto);
			Mockito.when(databaseGroups.get(groupId)).thenReturn(dbGroup);
		}

		Map<String, Authority> databaseUsers = new HashMap<>();
		var userMock = Mockito.mock(User.class);
		Mockito.when(userMock.getId()).thenReturn("U_user");
		databaseUsers.put("U_user", userMock);
		databaseUsers.put("U_otherKC", otherKCUser);

		remoteUserPuller.syncUpdatedGroups(keycloakGroups, databaseGroups, Arrays.stream(deletedGroupIdString).collect(Collectors.toSet()), databaseUsers);

		for (var groupId : updatedGroupIdString) {
			var dbGroup = databaseGroups.get(groupId);
			Mockito.verify(dbGroup).setName(String.format("name %s", groupId));
			Mockito.verify(dbGroup).setPictureUrl(String.format("pic %s", groupId));
			MatcherAssert.assertThat(dbGroupMembers, Matchers.containsInAnyOrder(userMock, otherKCUser));
		}
	}

	@Nested
	@DisplayName("Test write methods")
	class WriteMethods {

		private final RealmResource realm = Mockito.mock(RealmResource.class);
		private final UsersResource usersResource = Mockito.mock(UsersResource.class);
		private final GroupsResource groupsResource = Mockito.mock(GroupsResource.class);
		private final KeycloakRealmRoles realmRoles = Mockito.mock(KeycloakRealmRoles.class);

		@BeforeEach
		void setUp() {
			remoteUserPuller.realm = realm;
			remoteUserPuller.realmRoles = realmRoles;
			Mockito.lenient().when(realm.users()).thenReturn(usersResource);
			Mockito.lenient().when(realm.groups()).thenReturn(groupsResource);
		}

		@Test
		@DisplayName("createUser returns representation and syncs to db on 201")
		void testCreateUserSuccess() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(201);
			Mockito.when(response.getHeaderString("Location")).thenReturn("https://kc/admin/realms/test/users/newId");
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("newId")).thenReturn(userResource);
			var representation = new UserRepresentation();
			representation.setId("newId");
			representation.setUsername("newuser");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			Mockito.when(userRepo.findById("newId")).thenReturn(null);

			var result = remoteUserPuller.createUser("newuser", "e", "f", "l", "pw", null, Set.of());

			Assertions.assertEquals("newId", result.getId());
			Mockito.verify(userRepo).persist(any(User.class));
		}

		@Test
		@DisplayName("createUser throws ClientErrorException with USERNAME_EXISTS on 409")
		void testCreateUserConflict() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(409);
			Mockito.when(response.readEntity(String.class)).thenReturn("a user with the same username already exists");

			var e = Assertions.assertThrows(ClientErrorException.class, () -> remoteUserPuller.createUser("u", "e", "f", "l", "pw", null, Set.of()));
			Assertions.assertEquals("USERNAME_EXISTS", e.getMessage());
			Mockito.verify(userRepo, Mockito.never()).persist(any(User.class));
		}

		@Test
		@DisplayName("createUser throws InternalServerErrorException on unexpected status")
		void testCreateUserServerError() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(500);

			Assertions.assertThrows(InternalServerErrorException.class, () -> remoteUserPuller.createUser("u", "e", "f", "l", "pw", null, Set.of()));
		}

		@Test
		@DisplayName("deleteUser rejects read-only (federated) user before touching the db")
		void testDeleteUserRejectsReadOnly() {
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("fed")).thenReturn(userResource);
			Mockito.when(userResource.getFederatedIdentity()).thenReturn(List.of(new FederatedIdentityRepresentation()));

			Assertions.assertThrows(ForbiddenException.class, () -> remoteUserPuller.deleteUser("fed"));
			Mockito.verify(userRepo, Mockito.never()).deleteById(any());
		}

		@Test
		@DisplayName("deleteUser deletes from db before Keycloak")
		void testDeleteUserOrdering() {
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("u")).thenReturn(userResource);
			Mockito.when(userResource.getFederatedIdentity()).thenReturn(List.of());
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.delete("u")).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(204);

			remoteUserPuller.deleteUser("u");

			var inOrder = Mockito.inOrder(userRepo, usersResource);
			inOrder.verify(userRepo).deleteById("u");
			inOrder.verify(usersResource).delete("u");
		}

		@Test
		@DisplayName("updateUserRoles diffs roles to add and remove")
		void testUpdateUserRoles() {
			var dbUser = Mockito.mock(User.class);
			Mockito.when(userRepo.findByIdOptional("u")).thenReturn(Optional.of(dbUser));
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("u")).thenReturn(userResource);
			var roleMapping = Mockito.mock(RoleMappingResource.class);
			var realmLevel = Mockito.mock(RoleScopeResource.class);
			Mockito.when(userResource.roles()).thenReturn(roleMapping);
			Mockito.when(roleMapping.realmLevel()).thenReturn(realmLevel);
			Mockito.when(realmRoles.getRealmRole(any())).thenReturn(new RoleRepresentation());

			remoteUserPuller.updateUserRoles("u", Set.of(RealmRole.USER));

			var captor = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(dbUser).setRealmRoles(captor.capture());
			Assertions.assertArrayEquals(new String[]{"user"}, captor.getValue());
			Mockito.verify(realmLevel).add(Mockito.anyList());
			Mockito.verify(realmLevel).remove(Mockito.anyList());
		}

		@Test
		@DisplayName("addUserToGroup maps foreign-key violation to NotFoundException")
		void testAddUserToGroupForeignKeyViolation() {
			Mockito.doThrow(new PersistenceException()).when(groupRepo).addMember("g", "u");

			Assertions.assertThrows(NotFoundException.class, () -> remoteUserPuller.addUserToGroup("g", "u"));
			Mockito.verify(usersResource, Mockito.never()).get(any());
		}

		@Test
		@DisplayName("syncUser creates a new user when none exists")
		void testSyncUserCreates() {
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("u")).thenReturn(userResource);
			var representation = new UserRepresentation();
			representation.setId("u");
			representation.setUsername("name");
			representation.setEmail("mail");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			Mockito.when(userRepo.findById("u")).thenReturn(null);

			var result = remoteUserPuller.syncUser("u");

			Assertions.assertEquals("u", result.getId());
			Assertions.assertEquals("name", result.getName());
			Assertions.assertEquals("mail", result.getEmail());
			Mockito.verify(userRepo).persist(result);
		}

		@Test
		@DisplayName("syncUser updates an existing user without clearing its realm roles")
		void testSyncUserUpdatesWithoutClearingRoles() {
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("u")).thenReturn(userResource);
			var representation = new UserRepresentation();
			representation.setId("u");
			representation.setUsername("renamed");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			var existing = Mockito.mock(User.class);
			Mockito.when(userRepo.findById("u")).thenReturn(existing);

			remoteUserPuller.syncUser("u");

			Mockito.verify(existing).setName("renamed");
			Mockito.verify(existing, Mockito.never()).setRealmRoles(any());
			Mockito.verify(userRepo).persist(existing);
		}

		@Test
		@DisplayName("syncGroup upserts the group and always invalidates effective membership")
		void testSyncGroupUpsertsAndInvalidates() {
			var groupResource = Mockito.mock(GroupResource.class);
			Mockito.when(groupsResource.group("g")).thenReturn(groupResource);
			var representation = new GroupRepresentation();
			representation.setId("g");
			representation.setName("gname");
			Mockito.when(groupResource.toRepresentation()).thenReturn(representation);
			Mockito.when(groupResource.members(0, KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of());
			Mockito.when(groupRepo.findById("g")).thenReturn(null);
			Mockito.when(userRepo.findByIds(Mockito.anyList())).thenReturn(Stream.of());

			var result = remoteUserPuller.syncGroup("g");

			Assertions.assertEquals("g", result.getId());
			Assertions.assertEquals("gname", result.getName());
			Mockito.verify(groupRepo).persist(result);
			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(List.of("g"));
		}

		@Test
		@DisplayName("syncGroup pages through members beyond a single Keycloak page")
		void testSyncGroupPaginatesMembers() {
			var groupResource = Mockito.mock(GroupResource.class);
			Mockito.when(groupsResource.group("g")).thenReturn(groupResource);
			var representation = new GroupRepresentation();
			representation.setId("g");
			representation.setName("g");
			Mockito.when(groupResource.toRepresentation()).thenReturn(representation);

			var firstPage = new ArrayList<UserRepresentation>();
			for (int i = 0; i < KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST; i++) {
				var member = new UserRepresentation();
				member.setId("u" + i);
				firstPage.add(member);
			}
			var lastMember = new UserRepresentation();
			lastMember.setId("uLast");
			Mockito.when(groupResource.members(0, KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST)).thenReturn(firstPage);
			Mockito.when(groupResource.members(KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST, KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(lastMember));
			Mockito.when(groupRepo.findById("g")).thenReturn(null);
			Mockito.when(userRepo.findByIds(Mockito.anyList())).thenReturn(Stream.of());

			remoteUserPuller.syncGroup("g");

			Mockito.verify(groupResource).members(KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST, KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST);
			var captor = ArgumentCaptor.forClass(List.class);
			Mockito.verify(userRepo).findByIds(captor.capture());
			Assertions.assertEquals(KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST + 1, captor.getValue().size());
		}

		@Test
		@DisplayName("createUser tolerates a failed group join and still creates the user")
		void testCreateUserToleratesGroupJoinFailure() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(201);
			Mockito.when(response.getHeaderString("Location")).thenReturn("https://kc/admin/realms/test/users/newId");
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("newId")).thenReturn(userResource);
			Mockito.doThrow(new InternalServerErrorException("kc down")).when(userResource).joinGroup("g1");
			var representation = new UserRepresentation();
			representation.setId("newId");
			representation.setUsername("u");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			Mockito.when(userRepo.findById("newId")).thenReturn(null);

			var result = remoteUserPuller.createUser("u", "e", "f", "l", "pw", null, Set.of("g1"));

			Assertions.assertEquals("newId", result.getId());
			Mockito.verify(userResource).joinGroup("g1");
			Mockito.verify(userRepo).persist(any(User.class));
			Mockito.verify(groupRepo, Mockito.never()).addMember(Mockito.eq("g1"), any());
			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(Set.of("g1"));
		}

		@Test
		@DisplayName("createUser writes membership only for the successfully joined groups on partial failure")
		void testCreateUserPartialGroupJoinFailure() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(201);
			Mockito.when(response.getHeaderString("Location")).thenReturn("https://kc/admin/realms/test/users/newId");
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("newId")).thenReturn(userResource);
			Mockito.doThrow(new InternalServerErrorException("kc down")).when(userResource).joinGroup("g2");
			var representation = new UserRepresentation();
			representation.setId("newId");
			representation.setUsername("u");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			Mockito.when(userRepo.findById("newId")).thenReturn(null);

			remoteUserPuller.createUser("u", "e", "f", "l", "pw", null, Set.of("g1", "g2"));

			Mockito.verify(groupRepo).addMember("g1", "newId");
			Mockito.verify(groupRepo, Mockito.never()).addMember(Mockito.eq("g2"), any());
			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(Set.of("g1", "g2"));
		}

		@Test
		@DisplayName("createUser writes DB group membership for successfully joined groups")
		void testCreateUserWritesGroupMembership() {
			var response = Mockito.mock(Response.class);
			Mockito.when(usersResource.create(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(201);
			Mockito.when(response.getHeaderString("Location")).thenReturn("https://kc/admin/realms/test/users/newId");
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("newId")).thenReturn(userResource);
			var representation = new UserRepresentation();
			representation.setId("newId");
			representation.setUsername("u");
			representation.setEnabled(true);
			Mockito.when(userResource.toRepresentation()).thenReturn(representation);
			Mockito.when(userRepo.findById("newId")).thenReturn(null);

			remoteUserPuller.createUser("u", "e", "f", "l", "pw", null, Set.of("g1"));

			var inOrder = Mockito.inOrder(userRepo, groupRepo, effectiveGroupMembershipRepo);
			inOrder.verify(userRepo).persist(any(User.class));
			inOrder.verify(groupRepo).addMember("g1", "newId");
			inOrder.verify(effectiveGroupMembershipRepo).updateGroups(Set.of("g1"));
		}

		@Test
		@DisplayName("updateUser rejects read-only (federated) user before mutating Keycloak")
		void testUpdateUserRejectsReadOnly() {
			var userResource = Mockito.mock(UserResource.class);
			Mockito.when(usersResource.get("fed")).thenReturn(userResource);
			Mockito.when(userResource.getFederatedIdentity()).thenReturn(List.of(new FederatedIdentityRepresentation()));

			Assertions.assertThrows(ForbiddenException.class, () -> remoteUserPuller.updateUser("fed", "e", "f", "l", null, null));
			Mockito.verify(userResource, Mockito.never()).update(any());
		}

		@Test
		@DisplayName("createGroup returns representation and syncs to db on 201")
		void testCreateGroupSuccess() {
			var response = Mockito.mock(Response.class);
			Mockito.when(groupsResource.add(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(201);
			Mockito.when(response.getHeaderString("Location")).thenReturn("https://kc/admin/realms/test/groups/newGroup");
			var groupResource = Mockito.mock(GroupResource.class);
			Mockito.when(groupsResource.group("newGroup")).thenReturn(groupResource);
			var representation = new GroupRepresentation();
			representation.setId("newGroup");
			representation.setName("New Group");
			Mockito.when(groupResource.toRepresentation()).thenReturn(representation);
			Mockito.when(groupResource.members(0, KeycloakAuthorityProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of());
			Mockito.when(groupRepo.findById("newGroup")).thenReturn(null);
			Mockito.when(userRepo.findByIds(Mockito.anyList())).thenReturn(Stream.of());

			var result = remoteUserPuller.createGroup("New Group", null);

			Assertions.assertEquals("newGroup", result.getId());
			Mockito.verify(groupRepo).persist(any(Group.class));
		}

		@Test
		@DisplayName("createGroup throws ClientErrorException with GROUP_NAME_EXISTS on 409")
		void testCreateGroupConflict() {
			var response = Mockito.mock(Response.class);
			Mockito.when(groupsResource.add(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(409);

			var e = Assertions.assertThrows(ClientErrorException.class, () -> remoteUserPuller.createGroup("g", null));
			Assertions.assertEquals("GROUP_NAME_EXISTS", e.getMessage());
			Mockito.verify(groupRepo, Mockito.never()).persist(any(Group.class));
		}

		@Test
		@DisplayName("createGroup throws InternalServerErrorException on unexpected status")
		void testCreateGroupServerError() {
			var response = Mockito.mock(Response.class);
			Mockito.when(groupsResource.add(any())).thenReturn(response);
			Mockito.when(response.getStatus()).thenReturn(500);

			Assertions.assertThrows(InternalServerErrorException.class, () -> remoteUserPuller.createGroup("g", null));
		}
	}

	@Nested
	@DisplayName("Test conditional membership invalidation")
	class ConditionalInvalidation {

		@Test
		@DisplayName("syncUpdatedGroups invalidates only groups whose membership changed")
		void testInvalidatesOnlyChangedGroups() {
			var member = Mockito.mock(User.class);
			Mockito.when(member.getId()).thenReturn("u");

			var unchanged = Mockito.mock(Group.class);
			Mockito.when(unchanged.getId()).thenReturn("unchanged");
			Mockito.when(unchanged.getMembers()).thenReturn(new HashSet<>(Set.of(member)));
			var changed = Mockito.mock(Group.class);
			Mockito.when(changed.getId()).thenReturn("changed");
			Mockito.when(changed.getMembers()).thenReturn(new HashSet<>());

			Map<String, Group> databaseGroups = Map.of("unchanged", unchanged, "changed", changed);
			var memberDto = new KeycloakUserDto("u", "n", "e", "f", "l", "p", true, Set.of());
			Map<String, KeycloakGroupDto> keycloakGroups = Map.of(
					"unchanged", new KeycloakGroupDto("unchanged", "unchanged", null, Set.of(memberDto)),
					"changed", new KeycloakGroupDto("changed", "changed", null, Set.of(memberDto))
			);
			Map<String, Authority> allAuthorities = Map.of("u", member);

			remoteUserPuller.syncUpdatedGroups(keycloakGroups, databaseGroups, Set.of(), allAuthorities);

			Mockito.verify(effectiveGroupMembershipRepo).updateGroups(Set.of("changed"));
		}
	}

	private static class StringArrayConverter extends SimpleArgumentConverter {
		@Override
		protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
			if (source instanceof String s && String[].class.isAssignableFrom(targetType)) {
				return s.split(",");
			} else {
				throw new IllegalArgumentException("Conversion not supported.");
			}
		}

	}

}