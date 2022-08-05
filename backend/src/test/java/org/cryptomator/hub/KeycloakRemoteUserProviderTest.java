package org.cryptomator.hub;

import io.quarkus.test.junit.QuarkusTest;
import org.cryptomator.hub.entities.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

@QuarkusTest
class KeycloakRemoteUserProviderTest {

	private RealmResource realm = Mockito.mock(RealmResource.class);
	private UsersResource usersResource = Mockito.mock(UsersResource.class);
	private KeycloakRemoteUserProvider keycloakRemoteUserProvider;
	private UserRepresentation user1 = Mockito.mock(UserRepresentation.class);
	private UserRepresentation user2 = Mockito.mock(UserRepresentation.class);
	private UserRepresentation syncer = Mockito.mock(UserRepresentation.class);

	@BeforeEach
	void setUp() {
		var synerConfig = Mockito.mock(SyncerConfig.class);

		Mockito.when(realm.users()).thenReturn(usersResource);

		Mockito.when(user1.getId()).thenReturn("id3000");
		Mockito.when(user1.getUsername()).thenReturn("username3000");
		Mockito.when(user1.getEmail()).thenReturn("email3000");
		Mockito.when(user1.getAttributes()).thenReturn(Map.of("picture", List.of("picture3000")));

		Mockito.when(user2.getId()).thenReturn("id3001");
		Mockito.when(user2.getUsername()).thenReturn("username3001");
		Mockito.when(user2.getEmail()).thenReturn("email3001");

		Mockito.when(syncer.getId()).thenReturn("idSyncer");
		Mockito.when(syncer.getUsername()).thenReturn("usernameSyncer");
		Mockito.when(synerConfig.getUsername()).thenReturn("usernameSyncer");

		keycloakRemoteUserProvider = new KeycloakRemoteUserProvider();
		keycloakRemoteUserProvider.syncerConfig = synerConfig;
	}

	@Test
	@DisplayName("test user listing excludes syncer and returns two users")
	public void testListUser() {
		Mockito.when(usersResource.list(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(user1, user2, syncer));

		var result = keycloakRemoteUserProvider.users(realm);

		Assertions.assertEquals(2, result.size());

		var resultUser1 = result.get(0);
		var resultUser2 = result.get(1);

		Assertions.assertEquals("id3000", resultUser1.id);
		Assertions.assertEquals("username3000", resultUser1.name);
		Assertions.assertEquals("email3000", resultUser1.email);
		Assertions.assertEquals("picture3000", resultUser1.pictureUrl);

		Assertions.assertEquals("id3001", resultUser2.id);
		Assertions.assertEquals("username3001", resultUser2.name);
		Assertions.assertEquals("email3001", resultUser2.email);
		Assertions.assertNull(resultUser2.pictureUrl);
	}

	@Test
	@DisplayName("test search user excludes syncer and returns two users")
	public void testSearchUser() {
		Mockito.when(usersResource.search("query")).thenReturn(List.of(user1, user2, syncer));

		var result = keycloakRemoteUserProvider.searchUser(realm, "query");

		Assertions.assertEquals(2, result.size());

		var resultUser1 = result.get(0);
		var resultUser2 = result.get(1);

		Assertions.assertEquals("id3000", resultUser1.id);
		Assertions.assertEquals("username3000", resultUser1.name);
		Assertions.assertEquals("email3000", resultUser1.email);
		Assertions.assertEquals("picture3000", resultUser1.pictureUrl);

		Assertions.assertEquals("id3001", resultUser2.id);
		Assertions.assertEquals("username3001", resultUser2.name);
		Assertions.assertEquals("email3001", resultUser2.email);
		Assertions.assertNull(resultUser2.pictureUrl);
	}


	@Nested
	@DisplayName("Test groups")
	public class Groups {

		private GroupsResource groupsResource = Mockito.mock(GroupsResource.class);
		private GroupRepresentation group1 = Mockito.mock(GroupRepresentation.class);
		private GroupRepresentation group2 = Mockito.mock(GroupRepresentation.class);

		private GroupResource groupResource1 = Mockito.mock(GroupResource.class);
		private GroupResource groupResource2 = Mockito.mock(GroupResource.class);


		public Groups() {
			Mockito.when(group1.getId()).thenReturn("grpId3000");
			Mockito.when(group1.getName()).thenReturn("grpName3000");

			Mockito.when(group2.getId()).thenReturn("grpId3001");
			Mockito.when(group2.getName()).thenReturn("grpName3001");

			Mockito.when(realm.groups()).thenReturn(groupsResource);
			Mockito.when(realm.groups().group("grpId3000")).thenReturn(groupResource1);
			Mockito.when(realm.groups().group("grpId3001")).thenReturn(groupResource2);

			Mockito.when(groupResource1.members(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of());
			Mockito.when(groupResource2.members(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(user1, user2));
		}

		@Test
		@DisplayName("test groups listing contains two groups with members in group2")
		public void testListGroups() {
			Mockito.when(groupsResource.groups(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(group1, group2));

			var result = keycloakRemoteUserProvider.groups(realm);

			Assertions.assertEquals(2, result.size());

			var resultGroup1 = result.get(0);
			var resultGroup2 = result.get(1);

			Assertions.assertEquals("grpId3000", resultGroup1.id);
			Assertions.assertEquals("grpName3000", resultGroup1.name);
			Assertions.assertEquals(0, resultGroup1.members.size());

			Assertions.assertEquals("grpId3001", resultGroup2.id);
			Assertions.assertEquals("grpName3001", resultGroup2.name);
			Assertions.assertEquals(2, resultGroup2.members.size());

			var membersGroup2 = resultGroup2.members.stream().toList();
			var member1Group2 = (User) membersGroup2.get(1);
			var member2Group2 = (User) membersGroup2.get(0);

			Assertions.assertEquals("id3000", member1Group2.id);
			Assertions.assertEquals("username3000", member1Group2.name);
			Assertions.assertEquals("email3000", member1Group2.email);
			Assertions.assertEquals("picture3000", member1Group2.pictureUrl);

			Assertions.assertEquals("id3001", member2Group2.id);
			Assertions.assertEquals("username3001", member2Group2.name);
			Assertions.assertEquals("email3001", member2Group2.email);
			Assertions.assertNull(member2Group2.pictureUrl);
		}

		@Test
		@DisplayName("test groups search contains two groups")
		public void testSearchGroups() {
			Mockito.when(groupsResource.groups(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(group1, group2));

			var result = keycloakRemoteUserProvider.searchGroup(realm, "grp");
			var result2 = keycloakRemoteUserProvider.searchGroup(realm, "GRP");

			Assertions.assertEquals(result, result2);

			Assertions.assertEquals(2, result.size());

			var resultGroup1 = result.get(0);
			var resultGroup2 = result.get(1);

			Assertions.assertEquals("grpId3000", resultGroup1.id);
			Assertions.assertEquals("grpName3000", resultGroup1.name);

			Assertions.assertEquals("grpId3001", resultGroup2.id);
			Assertions.assertEquals("grpName3001", resultGroup2.name);
		}
	}

}