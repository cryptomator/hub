package org.cryptomator.hub.keycloak;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock();
			Map<String, User> databaseUsers = Mockito.mock();

			var keycloakUserIds = Set.of(keycloakUserIdString);
			var databaseUserIds = Set.of(databaseUserIdString);
			var addedUserIds = Set.of(addedUserIdString);

			Mockito.when(keycloakUsers.keySet()).thenReturn(keycloakUserIds);
			Mockito.when(databaseUsers.keySet()).thenReturn(databaseUserIds);

			for (var userId : addedUserIds) {
				var keycloakUser = new KeycloakUserDto(userId, "name " + userId, "email " + userId, "pic " + userId);
				Mockito.when(keycloakUsers.get(userId)).thenReturn(keycloakUser);
			}

			var added = remoteUserPuller.syncAddedUsers(keycloakUsers, databaseUsers);

			Assertions.assertEquals(added.keySet(), addedUserIds);
			Mockito.verify(userRepo).persist(Mockito.<Iterable<User>>any());
			Mockito.verify(effectiveGroupMembershipRepo).updateUsers(Mockito.argThat(addedUserIds::containsAll));
			for (var userId : addedUserIds) {
				MatcherAssert.assertThat(persistedUsers, Matchers.hasItem(
						Matchers.allOf(
								Matchers.hasProperty("id", Matchers.equalTo(userId)),
								Matchers.hasProperty("name", Matchers.equalTo("name " + userId)),
								Matchers.hasProperty("email", Matchers.equalTo("email " + userId)),
								Matchers.hasProperty("pictureUrl", Matchers.equalTo("pic " + userId))
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
		public void testDeleteUsers(@ConvertWith(StringArrayConverter.class) String[] keycloakUserIdString, @ConvertWith(StringArrayConverter.class) String[] databaseUserIdString, @ConvertWith(StringArrayConverter.class) String[] deletedUserIdString) {
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
			Map<String, KeycloakUserDto> keycloakUsers = Mockito.mock();
			Map<String, User> databaseUsers = Mockito.mock();

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
				var dto = new KeycloakGroupDto(gid, "name " + gid, "pic " + gid, Set.of(kcUsers.get(gid)));
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

			var added = remoteUserPuller.syncAddedGroups(keycloakGroups, databaseGroups, databaseUsers);

			var addedGroupIds = Set.of(addedGroupIdString);
			Assertions.assertEquals(added.keySet(), addedGroupIds);
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
		public void testDeleteGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString) {
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
	public void testUpdateGroups(@ConvertWith(StringArrayConverter.class) String[] keycloakGroupIdString, @ConvertWith(StringArrayConverter.class) String[] databaseGroupIdString, @ConvertWith(StringArrayConverter.class) String[] deletedGroupIdString, @ConvertWith(StringArrayConverter.class) String[] updatedGroupIdString) {
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
			Mockito.verify(dbGroup).setPictureUrl(String.format("pic %s", groupId));
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