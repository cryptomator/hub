package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
@DisplayName("Resource /users")
public class UsersResourceIT {

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

}