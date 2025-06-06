package org.cryptomator.hub.keycloak;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

class KeycloakAuthorityPullerTest {

	private final KeycloakAuthorityProvider remoteUserProvider = Mockito.mock(KeycloakAuthorityProvider.class);
	private final User.Repository userRepo = Mockito.mock(User.Repository.class);
	private final Group.Repository groupRepo = Mockito.mock(Group.Repository.class);

	private KeycloakAuthorityPuller remoteUserPuller;

	@BeforeEach
	void setUp() {
		remoteUserPuller = new KeycloakAuthorityPuller();
		remoteUserPuller.remoteUserProvider = remoteUserProvider;
		remoteUserPuller.userRepo = userRepo;
		remoteUserPuller.groupRepo = groupRepo;
		Mockito.doNothing().when(userRepo).persist((User) Mockito.any());
		Mockito.doNothing().when(groupRepo).persist((Group) Mockito.any());
	}

	@Nested
	@DisplayName("Test add/delete Users")
	public class AddDeleteUsers {

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
		public void testAddUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] addedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			var keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			var databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			var addedUserIds = Arrays.stream(addedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (var userId : addedUserIds) {
				var keycloakUser = new KeycloakUserDto(userId, "name " + userId, "email " + userId, "pic " + userId);
				Mockito.when(keycloakUsers.get(userId)).thenReturn(keycloakUser);
			}

			remoteUserPuller.syncAddedUsers(keycloakUsers, databaseUsers);

			for (var userId : addedUserIds) {
				Mockito.verify(userRepo).persist(argThat((User u) ->
						u.getId().equals(userId)
								&& u.getName().equals("name " + userId)
								&& u.getEmail().equals("email " + userId)
								&& u.getPictureUrl().equals("pic " + userId)
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
		public void testDeleteUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			var keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			var databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			var deletedMap = Arrays.stream(deletedUserIdString).collect(Collectors.toMap(Function.identity(), id -> Mockito.mock(User.class)));

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			deletedMap.forEach((id, user) ->
					Mockito.when(databaseUsers.get(id)).thenReturn(user));

			var result = remoteUserPuller.syncDeletedUsers(keycloakUsers, databaseUsers);

			for (var id : deletedUserIdString) {
				Mockito.verify(userRepo).delete(deletedMap.get(id));
			}

			var expected = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());
			MatcherAssert.assertThat(result, Matchers.equalTo(expected));
		}
	}

	@Nested
	@DisplayName("Test update Users")
	public class UpdateUsers {

		@DisplayName("test update users")
		@ParameterizedTest(name = "KCUser: {0} DBUser: {1} Deleted: {2} Updated: {3}")
		@CsvSource(value = {
				"foo,bar,baz;foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;la,li,lu;,", // foo,bar,baz new, la,li,lu removed
				",;foo,bar,baz;foo,bar,baz;,", // all removed
				"foo,bar,baz;,;,;,", // all new
				",;,;,;," // all empty
		}, delimiterString = ";")
		public void testUpdateUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString, @ConvertWith(StringArrayConverter.class) String[] updatedUserIdString) {
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			var keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			var databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			var deletedUserIds = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());
			var updatedUserIds = Arrays.stream(updatedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (var userId : updatedUserIds) {
				var keycloakUser = new KeycloakUserDto(userId, "name " + userId, "email " + userId, "pic " + userId);
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
				Mockito.verify(databaseUser).setPictureUrl("pic " + userId);
			}
			Mockito.verify(userRepo, Mockito.never()).persist(any(User.class));
		}
	}

	@Nested
	@DisplayName("Test add/delete Groups")
	public class AddDeleteGroups {

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
		public void testAddGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] addedGroupIdString) {
			Map<String, KeycloakUserDto> kcUsers = new HashMap<>();
			for (var gid : keycloakGroupIdString) {
				kcUsers.put(gid, new KeycloakUserDto(gid, "name " + gid, "email " + gid, "pic " + gid));
			}

			Map<String, KeycloakGroupDto> keycloakGroups = new HashMap<>();
			for (var gid : keycloakGroupIdString) {
				var dto = new KeycloakGroupDto(gid, "Name " + gid, Set.of(kcUsers.get(gid)));
				keycloakGroups.put(gid, dto);
			}

			Map<String, User> databaseUsers = new HashMap<>();
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

			remoteUserPuller.syncAddedGroups(keycloakGroups, databaseGroups, databaseUsers);

			for (var newGid : addedGroupIdString) {
				Mockito.verify(groupRepo).persist(argThat((Group created) -> {
					if (!created.getId().equals(newGid)) {
						return false;
					}
					var members = created.getMembers();
					return members.stream().anyMatch(m -> m.getId().equals(newGid));
				}));
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
		public void testDeleteGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString) {
			Map<String, KeycloakGroupDto> keycloakGroups = Mockito.mock(Map.class);
			Map<String, Group> databaseGroups = Mockito.mock(Map.class);

			var kcGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
			var dbGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
			var deletedMap = Arrays.stream(deletedGroupIdString).collect(Collectors.toMap(Function.identity(), id -> Mockito.mock(Group.class)));

			Mockito.when(keycloakGroups.keySet()).thenReturn(kcGroupIds);
			Mockito.when(databaseGroups.keySet()).thenReturn(dbGroupIds);

			deletedMap.forEach((id, group) -> Mockito.when(databaseGroups.get(id)).thenReturn(group));

			var result = remoteUserPuller.syncDeletedGroups(keycloakGroups, databaseGroups);

			for (var id : deletedGroupIdString) {
				Mockito.verify(groupRepo).delete(deletedMap.get(id));
			}
			var expected = Arrays.stream(deletedGroupIdString).collect(Collectors.toSet());
			MatcherAssert.assertThat(result, Matchers.equalTo(expected));
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
	public void testUpdateGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString, @ConvertWith(StringArrayConverter.class) String[] updatedGroupIdString) {
		var dbOnlyUser = Mockito.mock(User.class);
		Mockito.when(dbOnlyUser.getId()).thenReturn("U_dbOnly");
		var otherKCUser = Mockito.mock(User.class);
		Mockito.when(otherKCUser.getId()).thenReturn("U_otherKC");

		Map<String, KeycloakGroupDto> keycloakGroups = Mockito.mock(Map.class);
		Map<String, Group> databaseGroups = Mockito.mock(Map.class);

		var keycloakGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
		var databaseGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
		var updatedGroupIds = Arrays.stream(updatedGroupIdString).collect(Collectors.toSet());

		Mockito.when(keycloakGroups.keySet()).thenReturn(keycloakGroupIds);
		Mockito.when(databaseGroups.keySet()).thenReturn(databaseGroupIds);

		var dbGroupMembers = new HashSet<Authority>(Set.of(dbOnlyUser));
		for (var groupId : updatedGroupIds) {
			var kcDto = Mockito.mock(KeycloakGroupDto.class);
			Mockito.when(kcDto.name()).thenReturn(String.format("name %s", groupId));
			Mockito.when(kcDto.members()).thenReturn(Set.of(
					new KeycloakUserDto("U_user", "n", "e", "p"),
					new KeycloakUserDto("U_otherKC", "n", "e", "p")
			));

			var dbGroup = Mockito.mock(Group.class);
			Mockito.when(dbGroup.getId()).thenReturn(groupId);
			Mockito.when(dbGroup.getMembers()).thenReturn(dbGroupMembers);

			Mockito.when(keycloakGroups.get(groupId)).thenReturn(kcDto);
			Mockito.when(databaseGroups.get(groupId)).thenReturn(dbGroup);
		}

		Map<String, User> databaseUsers = new HashMap<>();
		var userMock = Mockito.mock(User.class);
		Mockito.when(userMock.getId()).thenReturn("U_user");
		databaseUsers.put("U_user", userMock);
		databaseUsers.put("U_otherKC", otherKCUser);

		remoteUserPuller.syncUpdatedGroups(keycloakGroups, databaseGroups, Arrays.stream(deletedGroupIdString).collect(Collectors.toSet()), databaseUsers);

		for (var groupId : updatedGroupIdString) {
			var dbGroup = databaseGroups.get(groupId);
			Mockito.verify(dbGroup).setName(String.format("name %s", groupId));
			MatcherAssert.assertThat(dbGroupMembers, Matchers.containsInAnyOrder(userMock, otherKCUser));
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