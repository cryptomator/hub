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
import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.keycloak.KeycloakAdminService;
import org.junit.jupiter.api.AfterAll;
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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Resource /groups")
public class GroupsResourceIT {

	@Inject
	AgroalDataSource dataSource;

	@InjectMock
	KeycloakAdminService keycloakAdminService;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsAuthorzedUser1 {

		@Test
		@DisplayName("GET /groups returns 200")
		public void testGetAll() {
			when().get("/groups")
					.then().statusCode(200)
					.body("id", hasItems("group1", "group2"));
		}

		@Test
		@DisplayName("GET /groups/group1/effective-members contains direct and subgroup members")
		public void testGetEffectiveUsers() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('user999', 'USER', 'User 999'),
							('group999', 'GROUP', 'Group 999');

						INSERT INTO "user_details" ("id") VALUES ('user999');
						INSERT INTO "group_details" ("id") VALUES ('group999');

						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group999', 'user999'),
							('group1', 'group999');
						""");
			}

			when().get("/groups/{groupId}/effective-members", "group1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user999"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'user999' OR "id" = 'group999';
						""");
			}
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
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

	@Nested
	@DisplayName("Group CRUD Operations")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public class GroupCrudOperations {

		@BeforeEach
		public void resetMocks() {
			Mockito.reset(keycloakAdminService);
		}

		@BeforeAll
		public void setup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('newGroupId123', 'GROUP', 'New Group');
						INSERT INTO "group_details" ("id") VALUES ('newGroupId123');
						""");
			}
		}

		@AfterAll
		public void tearDown() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'newGroupId123';
						""");
			}
		}

		@Test
		@DisplayName("POST /groups returns 201 when group created successfully")
		public void testCreateGroupSuccess() {
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
		public void testCreateGroupConflict() {
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
		public void testCreateGroupInvalidBody() {
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
		public void testGetGroupSuccess() {
			var groupRep = new GroupRepresentation();
			groupRep.setId("group1");
			groupRep.setName("Group 1");

			Mockito.when(keycloakAdminService.getGroup("group1")).thenReturn(groupRep);

			when().get("/groups/group1")
					.then().statusCode(200)
					.body("id", is("group1"));
		}

		@Test
		@DisplayName("GET /groups/{id} returns 404 for non-existing group")
		public void testGetGroupNotFound() {
			when().get("/groups/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("PUT /groups/{id} returns 200 when update successful")
		public void testUpdateGroupSuccess() {
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
		public void testUpdateGroupNotFound() {
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
		public void testDeleteGroupSuccess() {
			Mockito.doNothing().when(keycloakAdminService).deleteGroup("group2");

			when().delete("/groups/group2")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).deleteGroup("group2");
		}

		@Test
		@DisplayName("DELETE /groups/{id} returns 404 for non-existing group")
		public void testDeleteGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).deleteGroup("nonexistent");

			when().delete("/groups/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 204 when member added")
		public void testAddMemberSuccess() {
			Mockito.doNothing().when(keycloakAdminService).addUserToGroup("group1", "user2");

			when().post("/groups/group1/members/user2")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).addUserToGroup("group1", "user2");
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 404 for non-existing group")
		public void testAddMemberGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).addUserToGroup("nonexistent", "user1");

			when().post("/groups/nonexistent/members/user1")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /groups/{groupId}/members/{userId} returns 404 for non-existing user")
		public void testAddMemberUserNotFound() {
			Mockito.doThrow(new NotFoundException("User not found"))
					.when(keycloakAdminService).addUserToGroup("group1", "nonexistent");

			when().post("/groups/group1/members/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 204 when member removed")
		public void testRemoveMemberSuccess() {
			Mockito.doNothing().when(keycloakAdminService).removeUserFromGroup("group1", "user1");

			when().delete("/groups/group1/members/user1")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).removeUserFromGroup("group1", "user1");
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 404 for non-existing group")
		public void testRemoveMemberGroupNotFound() {
			Mockito.doThrow(new NotFoundException("Group not found"))
					.when(keycloakAdminService).removeUserFromGroup("nonexistent", "user1");

			when().delete("/groups/nonexistent/members/user1")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /groups/{groupId}/members/{userId} returns 404 for non-existing user")
		public void testRemoveMemberUserNotFound() {
			Mockito.doThrow(new NotFoundException("User not found"))
					.when(keycloakAdminService).removeUserFromGroup("group1", "nonexistent");

			when().delete("/groups/group1/members/nonexistent")
					.then().statusCode(404);
		}

	}

}