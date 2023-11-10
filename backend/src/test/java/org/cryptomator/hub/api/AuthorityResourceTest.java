package org.cryptomator.hub.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@DisplayName("Resource /authorities")
public class AuthorityResourceTest {

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("Search")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class Search {

		@Test
		@DisplayName("GET /search?query=U returns 200 with \"user1\", \"user2\", \"group1\"")
		public void testGetAll() {
			when().get("/authorities/search?query=U")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2", "group1"));
		}

		@Test
		@DisplayName("GET /search?query=u returns 200 with \"user1\", \"user2\", \"group1\"")
		public void testGetAllIgnoreCase() {
			when().get("/authorities/search?query=u")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2", "group1"));
		}

		@Test
		@DisplayName("GET /search?query=User returns 200 with \"user1\", \"user2\"")
		public void testGetUser() {
			when().get("/authorities/search?query=User")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2"));
		}

		@Test
		@DisplayName("GET /search?query=Group Name 1 returns 200 with \"group1\"")
		public void testGetExactMatch() {
			when().get("/authorities/search?query=Group Name 1")
					.then().statusCode(200)
					.body("id", hasItems("group1"));
		}

		@Test
		@DisplayName("GET /search?query=User Name 3000 returns 200 with empty body")
		public void testGetEmpty() {
			when().get("/authorities/search?query=User Name 3000")
					.then().statusCode(200)
					.body("id", empty());
		}

		@Test
		@DisplayName("GET /search?query=1 returns 200 with \"user1\", \"group1\"")
		public void testGetSameUserGroupName() {
			when().get("/authorities/search?query=1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "group1"));
		}
	}

	@Nested
	@DisplayName("GET /authorities?ids=...")
	@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class GetSome {

		@Test
		@DisplayName("GET /authorities returns 200 with empty body")
		public void testGetSomeEmpty() {
			when().get("/authorities")
					.then().statusCode(200)
					.body("", hasSize(0));
		}

		@Test
		@DisplayName("GET /authorities?ids=iDoNotExist returns 200 with empty body")
		public void testGetSomeNotExisting() {
			given().param("ids", "iDoNotExist")
					.when().get("/authorities")
					.then().statusCode(200)
					.body("", hasSize(0));
		}

		@Test
		@DisplayName("GET /authorities?ids=user1&ids=group2 returns 200 with body containing user1 and group2")
		public void testGetSome() {
			given().param("ids", "user1", "group2")
					.when().get("/authorities")
					.then().statusCode(200)
					.body("id", containsInAnyOrder("user1", "group2"));
		}
	}
}