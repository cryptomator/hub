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
import org.cryptomator.hub.license.LicenseHolder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("Resource /users")
public class UsersResourceIT {

	@Inject
	AgroalDataSource dataSource;

	@InjectMock
	LicenseHolder licenseHolder;

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
		public void testGetMe2() {
			when().get("/users/me?withDevices=true")
					.then().statusCode(200)
					.body("id", is("user1"))
					.body("devices.id", hasItems("device1"));
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
			Mockito.doReturn(true).when(licenseHolder).isSet(); // TODO
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

}