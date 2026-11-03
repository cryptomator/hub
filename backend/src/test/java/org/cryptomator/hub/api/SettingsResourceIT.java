package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Resource /settings")
public class SettingsResourceIT {

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As admin")
	@TestSecurity(user = "Admin", roles = {"user", "admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class AsAdmin {

		@Test
		@Order(1)
		@DisplayName("GET /settings returns 200")
		void testGetInitial() {
			when().get("/settings")
					.then().statusCode(200)
					.body("wotMaxDepth", is(3))
					.body("wotIdVerifyLen", is(2))
					.body("defaultRequiredEmergencyKeyShares", is(2))
					.body("allowChoosingEmergencyCouncil", is(false))
					.body("enableAutomaticAccessGrant", is(false))
					.body("automaticAccessGrantTrustThreshold", is(0))
					.body("allowAutomaticAccessGrantOverride", is(false));
		}

		@Test
		@Order(2)
		@DisplayName("PUT /settings returns 204 No Content")
		void testPut() {
			var dto = new SettingsResource.SettingsDto("42", 5, 8, true, 2, 3, false, Set.of(), true, 2, true);
			given().contentType(ContentType.JSON).body(dto)
					.when().put("/settings")
					.then().statusCode(204);
		}

		@Test
		@Order(3)
		@DisplayName("GET /settings returns 200")
		void testGetModify() {
			when().get("/settings")
					.then().statusCode(200)
					.body("wotMaxDepth", is(5))
					.body("wotIdVerifyLen", is(8))
					.body("enableAutomaticAccessGrant", is(true))
					.body("automaticAccessGrantTrustThreshold", is(2))
					.body("allowAutomaticAccessGrantOverride", is(true));
		}

		@Test
		@Order(4)
		@DisplayName("PUT /settings with trustThreshold=-1 (trust everyone) returns 204 No Content")
		void testPutTrustEveryone() {
			var dto = new SettingsResource.SettingsDto("42", 3, 2, true, 2, 3, false, Set.of(), true, -1, true);
			given().contentType(ContentType.JSON).body(dto)
					.when().put("/settings")
					.then().statusCode(204);
		}

		@Test
		@Order(5)
		@DisplayName("PUT /settings with trustThreshold=-2 returns 400 Bad Request")
		void testPutTrustThresholdTooLow() {
			var dto = new SettingsResource.SettingsDto("42", 3, 2, true, 2, 3, false, Set.of(), true, -2, true);
			given().contentType(ContentType.JSON).body(dto)
					.when().put("/settings")
					.then().statusCode(400);
		}

		@Test
		@Order(6)
		@DisplayName("PUT /settings with trustThreshold=10 returns 400 Bad Request")
		void testPutTrustThresholdTooHigh() {
			var dto = new SettingsResource.SettingsDto("42", 3, 2, true, 2, 3, false, Set.of(), true, 10, true);
			given().contentType(ContentType.JSON).body(dto)
					.when().put("/settings")
					.then().statusCode(400);
		}

		@Test
		@Order(7)
		@DisplayName("PUT /settings returns 204 No Content")
		void testPutBackToDefault() {
			var dto = new SettingsResource.SettingsDto("42", 3, 2, true, 2, 3, false, Set.of(), false, 0, false);
			given().contentType(ContentType.JSON).body(dto)
					.when().put("/settings")
					.then().statusCode(204);
		}


	}

	@Nested
	@DisplayName("As normal user")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsNormalUser {

		@Test
		@DisplayName("GET /settings returns 200")
		void testGet() {
			when().get("/settings")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("PUT /settings returns 403 Forbidden")
		void testPut() {
			given().contentType(ContentType.JSON).body("")
					.when().put("/settings")
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@Test
		@DisplayName("GET /billing returns 401 Unauthorized")
		void testGet() {
			when().get("/settings")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("PUT /settings returns 401 Unauthorized")
		void testPut() {
			given().contentType(ContentType.JSON).body("")
					.when().put("/settings")
					.then().statusCode(401);
		}

	}
}
