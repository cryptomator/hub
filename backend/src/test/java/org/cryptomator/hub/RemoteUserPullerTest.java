package org.cryptomator.hub;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.AuthorityRepository;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.UserRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class RemoteUserPullerTest {

	private final RemoteUserProvider remoteUserProvider = Mockito.mock(RemoteUserProvider.class);
	private final User user = Mockito.mock(User.class);
	private final AuthorityRepository authorityRepo = Mockito.mock(AuthorityRepository.class);
	private final UserRepository userRepo = Mockito.mock(UserRepository.class);

	private RemoteUserPuller remoteUserPuller;

	@BeforeEach
	void setUp() {
		remoteUserPuller = new RemoteUserPuller();
		remoteUserPuller.remoteUserProvider = remoteUserProvider;
		remoteUserPuller.authorityRepo = authorityRepo;
		remoteUserPuller.userRepo = userRepo;
		Mockito.doNothing().when(authorityRepo).persist((Authority) Mockito.any());
		Mockito.doNothing().when(userRepo).persist((User) Mockito.any());
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
				var authorityMock = Mockito.mock(TestAuthority.class);
				Mockito.when(authorityMock.getId()).thenReturn(authorityId);
				Mockito.when(keycloakAuthorities.get(authorityId)).thenReturn(authorityMock);
			}

			remoteUserPuller.syncAddedAuthorities(keycloakAuthorities, databaseAuthorities);

			for (String authorityId : addedAuthorityIds) {
				Mockito.verify(authorityRepo).persist(keycloakAuthorities.get(authorityId));
			}
		}

		@DisplayName("test delete authorities")
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
			Map<String, TestAuthority> deletedAuthorityIds = Arrays.stream(deletedAuthorityIdString).collect(Collectors.toMap(Function.identity(), (id) -> Mockito.mock(TestAuthority.class)));


			Mockito.when(keycloakAuthorities.keySet()).thenReturn(keycloakAuthorityIds);
			Mockito.when(databaseAuthorities.keySet()).thenReturn(databaseAuthorityIds);

			deletedAuthorityIds.forEach((id, authority) ->
					Mockito.when(keycloakAuthorities.get(id)).thenReturn(authority));

			remoteUserPuller.syncDeletedAuthorities(keycloakAuthorities, databaseAuthorities);

			deletedAuthorityIds.forEach((id, authority) ->
					Mockito.verify(authorityRepo).delete(authority));
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
				Mockito.when(kcUser.getPictureUrl()).thenReturn(String.format("picture %s", userId));
				Mockito.when(kcUser.getName()).thenReturn(String.format("name %s", userId));
				Mockito.when(kcUser.getEmail()).thenReturn(String.format("email %s", userId));

				Mockito.when(keycloakUsers.get(userId)).thenReturn(kcUser);
				Mockito.when(databaseUsers.get(userId)).thenReturn(Mockito.mock(User.class));
			}

			remoteUserPuller.syncUpdatedUsers(keycloakUsers, databaseUsers, deletedUserIds);

			for (String userId : updatedUserIds) {
				var dbUser = databaseUsers.get(userId);
				Mockito.verify(userRepo).persist(dbUser);
				Mockito.verify(dbUser).setPictureUrl(String.format("picture %s", userId));
				Mockito.verify(dbUser).setName(String.format("name %s", userId));
				Mockito.verify(dbUser).setEmail(String.format("email %s", userId));
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

			var dbGroupMembers = new HashSet<Authority>(Set.of(dbOnlyUser));
			for (String groupId : updatedGroupIds) {
				var kcGroup = Mockito.mock(Group.class, "kcGroup");
				Mockito.when(kcGroup.getName()).thenReturn(String.format("name %s", groupId));
				Mockito.when(kcGroup.getMembers()).thenReturn(Set.of(user, otherKCUser));

				var dbGroup = Mockito.mock(Group.class, "dbGroup");
				Mockito.when(dbGroup.getMembers()).thenReturn(dbGroupMembers);

				Mockito.when(keycloakGroups.get(groupId)).thenReturn(kcGroup);
				Mockito.when(databaseGroups.get(groupId)).thenReturn(dbGroup);
			}

			remoteUserPuller.syncUpdatedGroups(keycloakGroups, databaseGroups, deletedGroupIds);

			for (String groupId : updatedGroupIds) {
				var dbGroup = databaseGroups.get(groupId);
				Mockito.verify(dbGroup).setName(String.format("name %s", groupId));
				MatcherAssert.assertThat(dbGroupMembers, Matchers.containsInAnyOrder(user, otherKCUser));
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