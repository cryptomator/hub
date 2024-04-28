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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
				"GET, /users"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

}