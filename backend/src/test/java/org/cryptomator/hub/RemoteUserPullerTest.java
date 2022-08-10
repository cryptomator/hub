package org.cryptomator.hub;

import io.quarkus.test.junit.QuarkusTest;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.junit.jupiter.api.Assertions;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@QuarkusTest
class RemoteUserPullerTest {

	private RemoteUserPuller remoteUserPuller;
	private RemoteUserProvider remoteUserProvider = Mockito.mock(RemoteUserProvider.class);
	private User user = Mockito.mock(User.class);

	@BeforeEach
	void setUp() {
		remoteUserPuller = new RemoteUserPuller();
		remoteUserPuller.remoteUserProvider = remoteUserProvider;
	}

	@Nested
	@DisplayName("Test sync users")
	public class TestUser {

		@DisplayName("test add users")
		@ParameterizedTest(name = "KCUsers: {0} DBUsers: {1} AddedUsers: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;foo,bar,baz",
				"foo,bar,baz;la,bar,baz;foo",
				"baz;foo,bar,baz;,",
				",;foo,bar,baz;,",
				",;,;,"
		}, delimiterString = ";")
		public void testAddUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] addedUserIdString) {
			Map<String, User> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			Set<String> keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			Set<String> databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			Set<String> addedUserIds = Arrays.stream(addedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (String userId : addedUserIds) {
				Mockito.when(keycloakUsers.get(userId)).thenReturn(Mockito.mock(User.class));
			}

			remoteUserPuller.syncAddedUsers(keycloakUsers, databaseUsers);

			for (String userId : addedUserIds) {
				Mockito.verify(keycloakUsers.get(userId)).persist();
			}
		}

		@DisplayName("test delete users")
		@ParameterizedTest(name = "KCUsers: {0} DBUsers: {1} DeletedUsers: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;,",
				"foo,bar,baz;la,li,lu;la,li,lu",
				"foo,bar,baz;la,bar,baz;la",
				"baz;foo,bar,baz;foo,bar",
				",;foo,bar,baz;foo,bar,baz",
				",;,;,"
		}, delimiterString = ";")
		public void testDeleteUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString) {
			Map<String, User> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			Set<String> keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			Set<String> databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			Set<String> deletedUserIds = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (String userId : deletedUserIds) {
				Mockito.when(databaseUsers.get(userId)).thenReturn(Mockito.mock(User.class));
			}

			remoteUserPuller.syncDeletedUsers(keycloakUsers, databaseUsers);

			for (String userId : deletedUserIds) {
				Mockito.verify(databaseUsers.get(userId)).delete();
			}
		}

		@DisplayName("test update users")
		@ParameterizedTest(name = "KCUsers: {0} DBUsers: {1} DeletedUsers: {2} UpdatedUsers: {3} ")
		@CsvSource(value = {
				"foo,bar,baz;foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;la,li,lu;,", // foo,bar,baz new, la,li,lu removed
				",;foo,bar,baz;foo,bar,baz;,", // all removed
				"foo,bar,baz;,;,;,", // all new
				",;,;,;," // all empty
		}, delimiterString = ";")
		public void testUpdateUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString, @ConvertWith(StringArrayConverter.class) String[] updatedUserIdString) {
			Map<String, User> keycloakUsers = Mockito.mock(Map.class);
			Map<String, User> databaseUsers = Mockito.mock(Map.class);

			Set<String> keycloakUserIds = Arrays.stream(keycloakUserIdString).collect(Collectors.toSet());
			Set<String> databaseUserIds = Arrays.stream(databaseUserIdString).collect(Collectors.toSet());
			Set<String> deletedUserIds = Arrays.stream(deletedUserIdString).collect(Collectors.toSet());
			Set<String> updatedUserIds = Arrays.stream(updatedUserIdString).collect(Collectors.toSet());

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (String userId : updatedUserIds) {
				var kcUser = Mockito.mock(User.class);
				Mockito.when(kcUser.pictureUrl).thenReturn(String.format("picture %s", userId));
				Mockito.when(kcUser.name).thenReturn(String.format("name %s", userId));
				Mockito.when(kcUser.email).thenReturn(String.format("email %s", userId));

				Mockito.when(keycloakUsers.get(userId)).thenReturn(kcUser);
				Mockito.when(databaseUsers.get(userId)).thenReturn(Mockito.mock(User.class));
			}

			remoteUserPuller.syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUserIds);

			for (String userId : updatedUserIds) {
				Mockito.verify(databaseUsers.get(userId)).persist();
				Mockito.verify(databaseUsers.get(userId)).pictureUrl = String.format("picture %s", userId);
				Mockito.verify(databaseUsers.get(userId)).name = String.format("name %s", userId);
				Mockito.verify(databaseUsers.get(userId)).email = String.format("email %s", userId);
			}
		}
	}

	@Nested
	@DisplayName("Test sync groups")
	public class TestGroups {

		@DisplayName("test add groups")
		@ParameterizedTest(name = "KCGroups: {0} DBGroups: {1} AddedGroups: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;foo,bar,baz",
				"foo,bar,baz;la,bar,baz;foo",
				"baz;foo,bar,baz;,",
				",;foo,bar,baz;,",
				",;,;,"
		}, delimiterString = ";")
		public void testAddGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] addedGroupIdString) {
			Map<String, Group> keycloakGroups = Mockito.mock(Map.class);
			Map<String, Group> databaseGroups = Mockito.mock(Map.class);

			Set<String> keycloakGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
			Set<String> databaseGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
			Set<String> addedGroupIds = Arrays.stream(addedGroupIdString).collect(Collectors.toSet());

			Mockito.when(keycloakGroups.keySet()).thenReturn(keycloakGroupIds);
			Mockito.when(databaseGroups.keySet()).thenReturn(databaseGroupIds);

			for (String groupId : addedGroupIds) {
				Mockito.when(keycloakGroups.get(groupId)).thenReturn(Mockito.mock(Group.class));
			}

			remoteUserPuller.syncAddedGroups(keycloakGroups, databaseGroups);

			for (String groupId : addedGroupIds) {
				Mockito.verify(keycloakGroups.get(groupId)).persist();
			}
		}

		@DisplayName("test delete groups")
		@ParameterizedTest(name = "KCGroups: {0} DBGroups: {1} DeletedGroups: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;,",
				"foo,bar,baz;la,li,lu;la,li,lu",
				"foo,bar,baz;la,bar,baz;la",
				"baz;foo,bar,baz;foo,bar",
				",;foo,bar,baz;foo,bar,baz",
				",;,;,"
		}, delimiterString = ";")
		public void testDeleteGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString) {
			Map<String, Group> keycloakGroups = Mockito.mock(Map.class);
			Map<String, Group> databaseGroups = Mockito.mock(Map.class);

			Set<String> keycloakGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
			Set<String> databaseGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
			Set<String> deletedGroupIds = Arrays.stream(deletedGroupIdString).collect(Collectors.toSet());

			Mockito.when(keycloakGroups.keySet()).thenReturn(keycloakGroupIds);
			Mockito.when(databaseGroups.keySet()).thenReturn(databaseGroupIds);

			for (String groupId : deletedGroupIds) {
				Mockito.when(databaseGroups.get(groupId)).thenReturn(Mockito.mock(Group.class));
			}

			remoteUserPuller.syncDeletedGroups(keycloakGroups, databaseGroups);

			for (String groupId : deletedGroupIds) {
				Mockito.verify(databaseGroups.get(groupId)).delete();
			}
		}


		@DisplayName("test update groups")
		@ParameterizedTest(name = "KCGroups: {0} DBGroups: {1} DeletedGroups: {2} UpdatedGroups: {3} ")
		@CsvSource(value = {
				"foo,bar,baz;foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;la,li,lu;,", // foo,bar,baz new, la,li,lu removed
				",;foo,bar,baz;foo,bar,baz;,", // all removed
				"foo,bar,baz;,;,;,", // all new
				",;,;,;," // all empty
		}, delimiterString = ";")
		public void testUpdateGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString, @ConvertWith(StringArrayConverter.class) String[] updatedGroupIdString) {
			User dbOnlyUser = Mockito.mock(User.class);
			User otherKCUser = Mockito.mock(User.class);
			Map<String, Group> keycloakGroups = Mockito.mock(Map.class);
			Map<String, Group> databaseGroups = Mockito.mock(Map.class);

			Set<String> keycloakGroupIds = Arrays.stream(keycloakGroupIdString).collect(Collectors.toSet());
			Set<String> databaseGroupIds = Arrays.stream(databaseGroupIdString).collect(Collectors.toSet());
			Set<String> deletedGroupIds = Arrays.stream(deletedGroupIdString).collect(Collectors.toSet());
			Set<String> updatedGroupIds = Arrays.stream(updatedGroupIdString).collect(Collectors.toSet());

			Mockito.when(keycloakGroups.keySet()).thenReturn(keycloakGroupIds);
			Mockito.when(databaseGroups.keySet()).thenReturn(databaseGroupIds);

			for (String groupId : updatedGroupIds) {
				var kcGroup = Mockito.mock(Group.class);
				Mockito.when(kcGroup.name).thenReturn(String.format("name %s", groupId));
				Set<Authority> kcMembers = new HashSet<>(Arrays.asList(user, otherKCUser));
				Mockito.when(kcGroup.members).thenReturn(kcMembers);

				var dbGroup = Mockito.mock(Group.class);
				Mockito.when(dbGroup.name).thenReturn(String.format("name %s", groupId));
				Set<Authority> dbMembers = new HashSet<>(Collections.singletonList(dbOnlyUser));
				Mockito.when(dbGroup.members).thenReturn(dbMembers);

				Mockito.when(keycloakGroups.get(groupId)).thenReturn(kcGroup);
				Mockito.when(databaseGroups.get(groupId)).thenReturn(dbGroup);
			}

			remoteUserPuller.syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroupIds);

			for (String groupId : updatedGroupIds) {
				Mockito.verify(databaseGroups.get(groupId)).name = String.format("name %s", groupId);
				Assertions.assertEquals(Set.of(user, otherKCUser), databaseGroups.get(groupId).members);
			}
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