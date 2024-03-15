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
class RemoteUserPullerIT {

	private final RemoteUserProvider remoteUserProvider = Mockito.mock(RemoteUserProvider.class);
	private final User user = Mockito.mock(User.class);

	private RemoteUserPuller remoteUserPuller;

	@BeforeEach
	void setUp() {
		remoteUserPuller = new RemoteUserPuller();
		remoteUserPuller.remoteUserProvider = remoteUserProvider;
	}

	@Nested
	@DisplayName("Test add delete TestAuthorities")
	public class AddDeleteTestAuthorities {

		@DisplayName("test add authority")
		@ParameterizedTest(name = "KCAuthorities: {0} DBAuthorities: {1} AddedAuthorities: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;foo,bar,baz",
				"foo,bar,baz;la,li,lu;foo,bar,baz",
				"foo,bar,baz;la,bar,baz;foo",
				"baz;foo,bar,baz;,",
				",;foo,bar,baz;,",
				",;,;,"
		}, delimiterString = ";")
		public void testAddAuthorities(@ConvertWith(StringArrayConverter.class) String[] keycloakAuthorityIdString, @ConvertWith(StringArrayConverter.class) String[] databaseAuthorityIdString, @ConvertWith(StringArrayConverter.class) String[] addedAuthorityIdString) {
			Map<String, TestAuthority> keycloakAuthorities = Mockito.mock(Map.class);
			Map<String, TestAuthority> databaseAuthorities = Mockito.mock(Map.class);

			Set<String> keycloakAuthorityIds = Arrays.stream(keycloakAuthorityIdString).collect(Collectors.toSet());
			Set<String> databaseAuthorityIds = Arrays.stream(databaseAuthorityIdString).collect(Collectors.toSet());
			Set<String> addedAuthorityIds = Arrays.stream(addedAuthorityIdString).collect(Collectors.toSet());

			Mockito.when(keycloakAuthorities.keySet()).thenReturn(keycloakAuthorityIds);
			Mockito.when(databaseAuthorities.keySet()).thenReturn(databaseAuthorityIds);

			for (String authorityId : addedAuthorityIds) {
				Mockito.when(keycloakAuthorities.get(authorityId)).thenReturn(Mockito.mock(TestAuthority.class));
			}

			remoteUserPuller.syncAddedAuthorities(keycloakAuthorities, databaseAuthorities);

			for (String authorityId : addedAuthorityIds) {
				Mockito.verify(keycloakAuthorities.get(authorityId)).persist();
			}
		}

		@DisplayName("test delete users/groups")
		@ParameterizedTest(name = "KCUAuthorities: {0} DBAuthorities: {1} DeletedAuthorities: {2}")
		@CsvSource(value = {
				"foo,bar,baz;,;,",
				"foo,bar,baz;la,li,lu;la,li,lu",
				"foo,bar,baz;la,bar,baz;la",
				"baz;foo,bar,baz;foo,bar",
				",;foo,bar,baz;foo,bar,baz",
				",;,;,"
		}, delimiterString = ";")
		public void testDeleteAuthorities(@ConvertWith(StringArrayConverter.class) String[] keycloakAuthorityIdString, @ConvertWith(StringArrayConverter.class) String[] databaseAuthorityIdString, @ConvertWith(StringArrayConverter.class) String[] deletedAuthorityIdString) {
			Map<String, TestAuthority> keycloakAuthorities = Mockito.mock(Map.class);
			Map<String, TestAuthority> databaseAuthorities = Mockito.mock(Map.class);

			Set<String> keycloakAuthorityIds = Arrays.stream(keycloakAuthorityIdString).collect(Collectors.toSet());
			Set<String> databaseAuthorityIds = Arrays.stream(databaseAuthorityIdString).collect(Collectors.toSet());
			Set<String> deletedAuthorityIds = Arrays.stream(deletedAuthorityIdString).collect(Collectors.toSet());

			Mockito.when(keycloakAuthorities.keySet()).thenReturn(keycloakAuthorityIds);
			Mockito.when(databaseAuthorities.keySet()).thenReturn(databaseAuthorityIds);

			for (String authorityId : deletedAuthorityIds) {
				Mockito.when(databaseAuthorities.get(authorityId)).thenReturn(Mockito.mock(TestAuthority.class));
			}

			remoteUserPuller.syncDeletedAuthorities(keycloakAuthorities, databaseAuthorities);

			for (String authorityId : deletedAuthorityIds) {
				Mockito.verify(databaseAuthorities.get(authorityId)).delete();
			}
		}

		private static class TestAuthority extends Authority {
		}
	}

	@Nested
	@DisplayName("Test update authorities")
	public class TestUpdateAuthorities {

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