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
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.keycloak.KeycloakAdminService;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("Resource /users")
public class UsersResourceIT {

	@Inject
	AgroalDataSource dataSource;

	@InjectMock
	LicenseHolder licenseHolder;

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
		@DisplayName("PUT /users/me returns 201")
		public void testSyncMe() {
			when().put("/users/me")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("GET /users/me returns 200")
		public void testGetMe1() {
			when().get("/users/me")
					.then().statusCode(200)
					.body("id", is("user1"))
					.body("devices.flatten()", empty());
		}

		@Test
		@DisplayName("GET /users/me?withDevices=true returns 200")
		public void testGetMe2() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "audit_event" (id, timestamp, type) VALUES (30000, '2020-02-20T20:20:24.242Z', 'VAULT_KEY_RETRIEVE');
						INSERT INTO "audit_event" (id, timestamp, type) VALUES (30001, '2020-02-20T20:20:24.242Z', 'VAULT_KEY_RETRIEVE');
						INSERT INTO "audit_event_vault_key_retrieve" (id, retrieved_by, vault_id, result, device_id, ip_address) VALUES (30000, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'SUCCESS', 'device1', '1.2.3.4');
						INSERT INTO "audit_event_vault_key_retrieve" (id, retrieved_by, vault_id, result, device_id, ip_address) VALUES (30001, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'SUCCESS', 'legacyDevice1', '1.2.3.4');
						""");
			}

			when().get("/users/me?withDevices=true")
					.then().statusCode(200)
					.body("id", is("user1"))
					.body("devices.find { it.id == 'device1' }.lastAccessTime", nullValue())
					.body("devices.find { it.id == 'device1' }.lastIpAddress", nullValue())
					.body("devices.find { it.id == 'device1' }.legacyDevice", equalTo(false))
					.body("devices.find { it.id == 'legacyDevice1' }", nullValue());

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "audit_event" WHERE id=30000;
						DELETE FROM "audit_event" WHERE id=30001;
						""");
			}
		}

		@Test
		@DisplayName("GET /users/me-with-legacy-devices-and-access returns 200")
		public void testGetMe3() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "audit_event" (id, timestamp, type) VALUES (30000, '2020-02-20T20:20:24.242Z', 'VAULT_KEY_RETRIEVE');
						INSERT INTO "audit_event" (id, timestamp, type) VALUES (30001, '2020-02-20T20:20:24.242Z', 'VAULT_KEY_RETRIEVE');
						INSERT INTO "audit_event_vault_key_retrieve" (id, retrieved_by, vault_id, result, device_id, ip_address) VALUES (30000, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'SUCCESS', 'device1', '1.2.3.4');
						INSERT INTO "audit_event_vault_key_retrieve" (id, retrieved_by, vault_id, result, device_id, ip_address) VALUES (30001, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'SUCCESS', 'legacyDevice1', '1.2.3.4');
						""");
			}

			when().get("/users/me-with-legacy-devices-and-access")
					.then().statusCode(200)
					.body("id", is("user1"))
					.body("devices.find { it.id == 'legacyDevice1' }.lastAccessTime", equalTo("2020-02-20T20:20:24.242Z"))
					.body("devices.find { it.id == 'legacyDevice1' }.lastIpAddress", equalTo("1.2.3.4"))
					.body("devices.find { it.id == 'legacyDevice1' }.legacyDevice", equalTo(true));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "audit_event" WHERE id=30000;
						DELETE FROM "audit_event" WHERE id=30001;
						""");
			}
		}

		@Test
		@DisplayName("GET /users returns 200")
		public void testGetAll() {
			when().get("/users")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2"));
		}

		@Test
		@DisplayName("POST /users/me/access-tokens returns 200")
		public void testPostAccessTokens1() {
			var body = """
					{
						"7E57C0DE-0000-4000-8000-000100001111": "jwe.jwe.jwe.vault1.user1",
						"7E57C0DE-0000-4000-8000-BADBADBADBAD": "noSuchVault"
					},
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/users/me/access-tokens")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("POST /users/me/access-tokens returns 200 for empty list")
		public void testPostAccessTokens2() {
			given().contentType(ContentType.JSON).body("{}")
					.when().post("/users/me/access-tokens")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("POST /users/me/access-tokens returns 400 for malformed body")
		public void testPostAccessTokens3() {
			given().contentType(ContentType.JSON).body("")
					.when().post("/users/me/access-tokens")
					.then().statusCode(400);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /users/me",
				"PUT, /users/me",
				"GET, /users"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

	@Nested
	@DisplayName("Test Web of Trust")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class WebOfTrust {

		private Instant testStart;

		@BeforeAll
		public void setup() throws SQLException {
			testStart = Instant.now();
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user997', 'USER', 'User 997');
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user998', 'USER', 'User 998');
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
						INSERT INTO "user_details" ("id", "ecdsa_publickey") VALUES ('user997', 'ecdsa_public997');
						INSERT INTO "user_details" ("id", "ecdsa_publickey") VALUES ('user998', 'ecdsa_public998');
						INSERT INTO "user_details" ("id", "ecdsa_publickey") VALUES ('user999', 'ecdsa_public999');
						""");
			}
		}

		@Test
		@Order(1)
		@DisplayName("PUT /users/trusted/user998 as user 997")
		@TestSecurity(user = "User 997", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user997")
		})
		public void test997Trusts998() {
			given().contentType(ContentType.TEXT).body("997 trusts 998")
					.when().put("/users/trusted/user998")
					.then().statusCode(204);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /users/trusted/user999 as user 998")
		@TestSecurity(user = "User 998", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user998")
		})
		public void test998Trusts999() {
			given().contentType(ContentType.TEXT).body("998 trusts 999")
					.when().put("/users/trusted/user999")
					.then().statusCode(204);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /users/trusted/user997 as user 998")
		@TestSecurity(user = "User 998", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user998")
		})
		public void test998Trusts997() {
			given().contentType(ContentType.TEXT).body("998 trusts 997")
					.when().put("/users/trusted/user997")
					.then().statusCode(204);
		}

		@Test
		@Order(2)
		@DisplayName("GET /users/trusted as user 997")
		@TestSecurity(user = "User 997", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user997")
		})
		public void testGetTrustedBy997() {
			given().when().get("/users/trusted")
					.then().statusCode(200)
					.body("$", hasSize(2))
					.body("trustedUserId", hasItems("user998", "user999"))
					.body("find{it.trustedUserId==\"user998\"}.signatureChain", hasItems("997 trusts 998"))
					.body("find{it.trustedUserId==\"user999\"}.signatureChain", hasItems("997 trusts 998", "998 trusts 999"));
		}

		@Test
		@Order(2)
		@DisplayName("GET /users/trusted as user 998")
		@TestSecurity(user = "User 998", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user998")
		})
		public void testGetTrustedBy998() {
			given().when().get("/users/trusted")
					.then().statusCode(200)
					.body("$", hasSize(2))
					.body("trustedUserId", hasItems("user997", "user999"))
					.body("find{it.trustedUserId==\"user997\"}.signatureChain", hasItems("998 trusts 997"))
					.body("find{it.trustedUserId==\"user999\"}.signatureChain", hasItems("998 trusts 999"));
		}

		@Test
		@Order(2)
		@DisplayName("GET /users/trusted as user 999")
		@TestSecurity(user = "User 999", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user999")
		})
		public void testGetTrustedBy999() {
			given().when().get("/users/trusted")
					.then().statusCode(200)
					.body("$", hasSize(0));
		}

		@Test
		@Order(3)
		@DisplayName("GET /users/trusted/user998 as user 997")
		@TestSecurity(user = "User 997", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user997")
		})
		public void test997Gets998() {
			given().when().get("/users/trusted/user998")
					.then().statusCode(200)
					.body("signatureChain", hasItems("997 trusts 998"));
		}

		@Test
		@Order(3)
		@DisplayName("GET /users/trusted/user999 as user 997")
		@TestSecurity(user = "User 997", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user997")
		})
		public void test997Gets999() {
			given().when().get("/users/trusted/user999")
					.then().statusCode(200)
					.body("signatureChain", hasItems("997 trusts 998", "998 trusts 999"));
		}

		@Test
		@Order(3)
		@DisplayName("GET /users/trusted/user998 as user 999")
		@TestSecurity(user = "User 999", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user999")
		})
		public void test999Gets998() {
			given().when().get("/users/trusted/user998")
					.then().statusCode(404);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "Admin", roles = {"admin"})
		@DisplayName("As admin, GET /auditlog contains signature events")
		public void testGetAuditLogEntries() {
			Mockito.doReturn(true).when(licenseHolder).isSet();
			Mockito.doReturn(false).when(licenseHolder).isExpired();

			given().param("startDate", DateTimeFormatter.ISO_INSTANT.format(testStart))
					.param("endDate", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
					.param("paginationId", 9999L)
					.when().get("/auditlog")
					.then().statusCode(200)
					.body("signature", contains("997 trusts 998", "998 trusts 999", "998 trusts 997"));
		}


		@AfterAll
		public void tearDown() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" IN ('user997', 'user998', 'user999');
						""");
			}
		}

	}

	@Nested
	@DisplayName("User CRUD Operations")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public class UserCrudOperations {

		@BeforeEach
		public void resetMocks() {
			Mockito.reset(keycloakAdminService);
		}

		@BeforeAll
		public void setup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('newUserId123', 'USER', 'newuser');
						INSERT INTO "user_details" ("id") VALUES ('newUserId123');
						""");
			}
		}

		@AfterAll
		public void tearDown() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'newUserId123';
						""");
			}
		}

		@Test
		@DisplayName("POST /users returns 201 when user created successfully")
		public void testCreateUserSuccess() {
			var userRep = new UserRepresentation();
			userRep.setId("newUserId123");
			userRep.setUsername("newuser");
			userRep.setEmail("newuser@example.com");

			Mockito.when(keycloakAdminService.createUser(
					Mockito.eq("newuser"),
					Mockito.eq("newuser@example.com"),
					Mockito.eq("New"),
					Mockito.eq("User"),
					Mockito.eq("password123"),
					Mockito.isNull(),
					Mockito.isNull()
			)).thenReturn(userRep);

			var body = """
					{
						"username": "newuser",
						"email": "newuser@example.com",
						"firstName": "New",
						"lastName": "User",
						"password": "password123"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/users")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("POST /users returns 409 when username already exists")
		public void testCreateUserConflictUsername() {
			Mockito.when(keycloakAdminService.createUser(
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.any(),
					Mockito.any()
			)).thenThrow(new ClientErrorException("USERNAME_EXISTS", Response.Status.CONFLICT));

			var body = """
					{
						"username": "existinguser",
						"email": "new@example.com",
						"firstName": "Test",
						"lastName": "User",
						"password": "password123"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/users")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("POST /users returns 409 when email already exists")
		public void testCreateUserConflictEmail() {
			Mockito.when(keycloakAdminService.createUser(
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.anyString(),
					Mockito.any(),
					Mockito.any()
			)).thenThrow(new ClientErrorException("EMAIL_EXISTS", Response.Status.CONFLICT));

			var body = """
					{
						"username": "newuser",
						"email": "existing@example.com",
						"firstName": "Test",
						"lastName": "User",
						"password": "password123"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/users")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("POST /users returns 400 for invalid body")
		public void testCreateUserInvalidBody() {
			var body = """
					{
						"username": "newuser"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().post("/users")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("GET /users/{id} returns 200 for existing user")
		public void testGetUserSuccess() {
			var userRep = new UserRepresentation();
			userRep.setId("user1");
			userRep.setUsername("User Name 1");
			userRep.setFirstName("User");
			userRep.setLastName("One");
			userRep.setCreatedTimestamp(1700000000000L);

			Mockito.when(keycloakAdminService.getUser("user1")).thenReturn(userRep);
			Mockito.when(keycloakAdminService.getUserRoles("user1")).thenReturn(Collections.emptySet());

			when().get("/users/user1")
					.then().statusCode(200)
					.body("id", is("user1"));
		}

		@Test
		@DisplayName("GET /users/{id} returns 404 for non-existing user")
		public void testGetUserNotFound() {
			when().get("/users/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("PUT /users/{id} returns 200 when update successful")
		public void testUpdateUserSuccess() {
			var userRep = new UserRepresentation();
			userRep.setId("user1");
			userRep.setUsername("User Name 1");
			userRep.setFirstName("Updated");
			userRep.setLastName("Name");

			Mockito.when(keycloakAdminService.updateUser(
					Mockito.eq("user1"),
					Mockito.eq("Updated"),
					Mockito.eq("Name"),
					Mockito.isNull(),
					Mockito.isNull()
			)).thenReturn(userRep);

			var body = """
					{
						"firstName": "Updated",
						"lastName": "Name"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().put("/users/user1")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("PUT /users/{id} returns 404 for non-existing user")
		public void testUpdateUserNotFound() {
			Mockito.when(keycloakAdminService.updateUser(
					Mockito.eq("nonexistent"),
					Mockito.any(),
					Mockito.any(),
					Mockito.any(),
					Mockito.any()
			)).thenThrow(new NotFoundException("User not found"));

			var body = """
					{
						"firstName": "Updated",
						"lastName": "Name"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().put("/users/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("PUT /users/{id} returns 403 for federated user")
		public void testUpdateUserForbidden() {
			Mockito.when(keycloakAdminService.updateUser(
					Mockito.eq("federatedUser"),
					Mockito.any(),
					Mockito.any(),
					Mockito.any(),
					Mockito.any()
			)).thenThrow(new ForbiddenException("User has a federated identity"));

			var body = """
					{
						"firstName": "Updated",
						"lastName": "Name"
					}
					""";
			given().contentType(ContentType.JSON).body(body)
					.when().put("/users/federatedUser")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("DELETE /users/{id} returns 204 when deleted successfully")
		public void testDeleteUserSuccess() {
			Mockito.doNothing().when(keycloakAdminService).deleteUser("user2");

			when().delete("/users/user2")
					.then().statusCode(204);

			Mockito.verify(keycloakAdminService).deleteUser("user2");
		}

		@Test
		@DisplayName("DELETE /users/{id} returns 404 for non-existing user")
		public void testDeleteUserNotFound() {
			Mockito.doThrow(new NotFoundException("User not found"))
					.when(keycloakAdminService).deleteUser("nonexistent");

			when().delete("/users/nonexistent")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("DELETE /users/{id} returns 403 for federated user")
		public void testDeleteUserForbidden() {
			Mockito.doThrow(new ForbiddenException("User has a federated identity"))
					.when(keycloakAdminService).deleteUser("federatedUser");

			when().delete("/users/federatedUser")
					.then().statusCode(403);
		}

	}

}