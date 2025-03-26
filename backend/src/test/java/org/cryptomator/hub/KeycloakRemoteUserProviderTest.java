package org.cryptomator.hub;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

class KeycloakRemoteUserProviderTest {

	private RealmResource realm = Mockito.mock(RealmResource.class);
	private UsersResource usersResource = Mockito.mock(UsersResource.class);
	private KeycloakRemoteUserProvider keycloakRemoteUserProvider;
	private UserRepresentation user1 = Mockito.mock(UserRepresentation.class);
	private UserRepresentation user2 = Mockito.mock(UserRepresentation.class);

	private UserRepresentation hubCliUser = Mockito.mock(UserRepresentation.class);

	private ClientsResource hubCliClientsResource = Mockito.mock(ClientsResource.class);

	private ClientRepresentation hubCliClientRepresentation = Mockito.mock(ClientRepresentation.class);

	private ClientResource hubCliClientResource = Mockito.mock(ClientResource.class);


	@BeforeEach
	void setUp() {
		var synerConfig = Mockito.mock(SyncerConfig.class);

		Mockito.when(realm.clients()).thenReturn(hubCliClientsResource);
		Mockito.when(realm.clients().findByClientId("cryptomatorhub-cli")).thenReturn(List.of());

		Mockito.when(realm.users()).thenReturn(usersResource);

		Mockito.when(user1.getId()).thenReturn("id3000");
		Mockito.when(user1.getUsername()).thenReturn("username3000");
		Mockito.when(user1.getEmail()).thenReturn("email3000");
		Mockito.when(user1.getAttributes()).thenReturn(Map.of("picture", List.of("picture3000")));

		Mockito.when(user2.getId()).thenReturn("id3001");
		Mockito.when(user2.getUsername()).thenReturn("username3001");
		Mockito.when(user2.getEmail()).thenReturn("email3001");

		keycloakRemoteUserProvider = new KeycloakRemoteUserProvider();
	}

	@Test
	@DisplayName("test user listing excludes syncer and returns two users")
	void testListUser() {
		Mockito.when(usersResource.list(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(user1, user2));

		var result = keycloakRemoteUserProvider.users(realm);

		Assertions.assertEquals(2, result.size());

		var resultUser1 = result.get(0);
		var resultUser2 = result.get(1);

		Assertions.assertEquals("id3000", resultUser1.getId());
		Assertions.assertEquals("username3000", resultUser1.getName());
		Assertions.assertEquals("email3000", resultUser1.getEmail());
		Assertions.assertEquals("picture3000", resultUser1.getPictureUrl());

		Assertions.assertEquals("id3001", resultUser2.getId());
		Assertions.assertEquals("username3001", resultUser2.getName());
		Assertions.assertEquals("email3001", resultUser2.getEmail());
		Assertions.assertNull(resultUser2.getPictureUrl());
	}

	@Test
	@DisplayName("test user listing excludes syncer, includes Hub CLI user and returns two users")
	void testListUserIncludingHubCliUser() {
		Mockito.when(usersResource.list(0, KeycloakRemoteUserProvider.MAX_COUNT_PER_REQUEST)).thenReturn(List.of(user1, user2));

		Mockito.when(realm.clients()).thenReturn(hubCliClientsResource);

		List<ClientRepresentation> clientRepresentations = Mockito.mock(List.class);
		Mockito.when(realm.clients().findByClientId("cryptomatorhub-cli")).thenReturn(clientRepresentations);
		Mockito.when(clientRepresentations.get(0)).thenReturn(hubCliClientRepresentation);

		Mockito.when(hubCliClientRepresentation.getId()).thenReturn("cryptomatorHubCliClientId");

		Mockito.when(realm.clients().get(Mockito.anyString())).thenReturn(hubCliClientResource);

		Mockito.when(hubCliUser.getId()).thenReturn("cryptomatorHubCliUserId");
		Mockito.when(hubCliUser.getUsername()).thenReturn("cryptomatorHubCliUserUsername");
		Mockito.when(hubCliClientResource.getServiceAccountUser()).thenReturn(hubCliUser);

		var result = keycloakRemoteUserProvider.users(realm);

		Assertions.assertEquals(3, result.size());

		var resultUser1 = result.get(0);
		var resultUser2 = result.get(1);
		var resultUser3 = result.get(2);

		Assertions.assertEquals("id3000", resultUser1.getId());
		Assertions.assertEquals("username3000", resultUser1.getName());
		Assertions.assertEquals("email3000", resultUser1.getEmail());
		Assertions.assertEquals("picture3000", resultUser1.getPictureUrl());

		Assertions.assertEquals("id3001", resultUser2.getId());
		Assertions.assertEquals("username3001", resultUser2.getName());
		Assertions.assertEquals("email3001", resultUser2.getEmail());
		Assertions.assertNull(resultUser2.getPictureUrl());

		Assertions.assertEquals("cryptomatorHubCliUserId", resultUser3.getId());
		Assertions.assertEquals("cryptomatorHubCliUserUsername", resultUser3.getName());
	}


	@Nested
	@DisplayName("Test groups")
	class Groups {

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

			Assertions.assertEquals("grpId3000", resultGroup1.getId());
			Assertions.assertEquals("grpName3000", resultGroup1.getName());
			Assertions.assertEquals(0, resultGroup1.getMembers().size());

			Assertions.assertEquals("grpId3001", resultGroup2.getId());
			Assertions.assertEquals("grpName3001", resultGroup2.getName());
			Assertions.assertEquals(2, resultGroup2.getMembers().size());

			var membersGroup2 = resultGroup2.getMembers().stream().sorted(Comparator.comparing(Authority::getId)).toList();
			var member1Group2 = (User) membersGroup2.get(0);
			var member2Group2 = (User) membersGroup2.get(1);

			Assertions.assertEquals("id3000", member1Group2.getId());
			Assertions.assertEquals("username3000", member1Group2.getName());
			Assertions.assertEquals("email3000", member1Group2.getEmail());
			Assertions.assertEquals("picture3000", member1Group2.getPictureUrl());

			Assertions.assertEquals("id3001", member2Group2.getId());
			Assertions.assertEquals("username3001", member2Group2.getName());
			Assertions.assertEquals("email3001", member2Group2.getEmail());
			Assertions.assertNull(member2Group2.getPictureUrl());
		}
	}

}