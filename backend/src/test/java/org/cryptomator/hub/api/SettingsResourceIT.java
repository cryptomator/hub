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
					.body("allowChoosingEmergencyCouncil", is(false));
		}

		@Test
		@Order(2)
		@DisplayName("PUT /settings returns 204 No Content")
		void testPut() {
			var dto = new SettingsResource.SettingsDto("42", 5, 8, true, 2, 3, false, Set.of());
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
					.body("wotIdVerifyLen", is(8));
		}

		@Test
		@Order(4)
		@DisplayName("PUT /settings returns 204 No Content")
		void testPutBackToDefault() {
			var dto = new SettingsResource.SettingsDto("42", 3, 2, true, 2, 3, false, Set.of());
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
