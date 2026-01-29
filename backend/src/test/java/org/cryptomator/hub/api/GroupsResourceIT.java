package org.cryptomator.hub.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.keycloak.KeycloakAdminService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Resource /groups")
public class GroupsResourceIT {

	@Inject
	AgroalDataSource dataSource;

	@Inject
	Group.Repository groupRepo;

	@Inject
	User.Repository userRepo;

	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;

	@InjectMock
	KeycloakAdminService keycloakAdminService;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	@Transactional
	void setupTestData() {
		var user999 = new User();
		user999.setId("user999");
		user999.setName("User 999");
		userRepo.persist(user999);

		var group999 = new Group();
		group999.setId("group999");
		group999.setName("Group 999");
		group999.getMembers().add(user999);
		groupRepo.persist(group999);

		var group1 = groupRepo.findById("group1");
		group1.getMembers().add(group999);
		groupRepo.persist(group1);

		effectiveGroupMembershipRepo.updateGroups(List.of("group1", "group999"));
	}

	@AfterEach
	@Transactional
	void cleanupTestData() {
		groupRepo.deleteById("group999");
		userRepo.deleteById("user999");
	}

	@Nested
	@DisplayName("As admin")
	@TestSecurity(user = "Admin User", roles = {"admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	public class AsAdmin {

		@Test
		@DisplayName("GET /groups returns 200")
		void testGetAll() {
			when().get("/groups")
					.then().statusCode(200)
					.body("id", hasItems("group1", "group2"));
		}

		@Test
		@DisplayName("GET /groups/group1/effective-members contains direct and subgroup members")
		void testGetEffectiveUsers() throws SQLException {
			when().get("/groups/{groupId}/effective-members", "group1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user999"));
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /groups"
		})
		void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

	@Nested
	@DisplayName("Group CRUD Operations")
	@TestSecurity(user = "Admin User", roles = {"admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public class GroupCrudOperations {

		@BeforeEach
		void resetMocks() {
			Mockito.reset(keycloakAdminService);
		}

		@BeforeAll
		void setup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('newGroupId123', 'GROUP', 'New Group');
						INSERT INTO "group_details" ("id") VALUES ('newGroupId123');
						""");
			}
		}

		@AfterAll
		void tearDown() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'newGroupId123';
						""");
			}
		}

		@Test
		@DisplayName("POST /groups returns 201 when group created successfully")
		void testCreateGroupSuccess() {
			var groupRep = new GroupRepresentation();
			groupRep.setId("newGroupId123");
			groupRep.setName("New Group");

			Mockito.when(keycloakAdminService.createGroup(
					Mockito.eq("New Group"),
					Mockito.isNull()
			)).thenReturn(groupRep);

			var body = """
					{
						"name": "New Group"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/groups")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("POST /groups returns 409 when group name already exists")
		void testCreateGroupConflict() {
			Mockito.when(keycloakAdminService.createGroup(
					Mockito.anyString(),
					Mockito.any()
			)).thenThrow(new jakarta.ws.rs.ClientErrorException("GROUP_NAME_EXISTS", jakarta.ws.rs.core.Response.Status.CONFLICT));

			var body = """
					{
						"name": "Existing Group"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/groups")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("POST /groups returns 400 for invalid body")
		void testCreateGroupInvalidBody() {
			var body = """
					{
						"pictureUrl": "http://example.com/pic.jpg"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/groups")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("GET /groups/{id} returns 200 for existing group")
		void testGetGroupSuccess() {
			when().get("/groups/group1")
					.then().statusCode(200)
					.body("id", is("group1"));
		}

		@Test
		@DisplayName("GET /groups/{id} returns 404 for non-existing group")
		void testGetGroupNotFound() {
			when().get("/groups/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("PUT /groups/{id} returns 200 when update successful")
		void testUpdateGroupSuccess() {
			var groupRep = new GroupRepresentation();
			groupRep.setId("group1");
			groupRep.setName("Updated Group");

			Mockito.when(keycloakAdminService.updateGroup(
					Mockito.eq("group1"),
					Mockito.eq("Updated Group"),
					Mockito.isNull()
			)).thenReturn(groupRep);

			var body = """
					{
						"name": "Updated Group"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().put("/groups/group1")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("PUT /groups/{id} returns 404 for non-existing group")
		void testUpdateGroupNotFound() {
			Mockito.when(keycloakAdminService.updateGroup(
					Mockito.eq("nonexistent"),
					Mockito.any(),
					Mockito.any()
			)).thenThrow(new NotFoundException("Group not found"));

			var body = """
					{
						"name": "Updated Group"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().put("/groups/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /groups/{id} returns 204 when deleted successfully")
		void testDeleteGroupSuccess() {
			Mockito.doNothing().when(keycloakAdminService).deleteGroup("mockedGroupId");

			when().delete("/groups/mockedGroupId")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).deleteGroup("mockedGroupId");
		}

		@Test
		@DisplayName("DELETE /groups/{id} returns 404 for non-existing group")
		void testDeleteGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).deleteGroup("nonexistent");

			when().delete("/groups/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 204 when member added")
		void testAddMemberSuccess() {
			Mockito.doNothing().when(keycloakAdminService).addUserToGroup("group1", "user2");

			when().post("/groups/group1/members/user2")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).addUserToGroup("group1", "user2");
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 404 for non-existing group")
		void testAddMemberGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).addUserToGroup("nonexistent", "user1");

			when().post("/groups/nonexistent/members/user1")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 404 for non-existing user")
		void testAddMemberUserNotFound() {
			Mockito.doThrow(new NotFoundException("User not found"))
					.when(keycloakAdminService).addUserToGroup("group1", "nonexistent");

			when().post("/groups/group1/members/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 204 when member removed")
		void testRemoveMemberSuccess() {
			Mockito.doNothing().when(keycloakAdminService).removeUserFromGroup("group1", "user1");

			when().delete("/groups/group1/members/user1")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).removeUserFromGroup("group1", "user1");
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 404 for non-existing group")
		void testRemoveMemberGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).removeUserFromGroup("nonexistent", "user1");

			when().delete("/groups/nonexistent/members/user1")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 404 for non-existing user")
		void testRemoveMemberUserNotFound() {
			Mockito.doThrow(new NotFoundException("User not found"))
					.when(keycloakAdminService).removeUserFromGroup("group1", "nonexistent");

			when().delete("/groups/group1/members/nonexistent")
					.then().statusCode(404);
		}

	}

}