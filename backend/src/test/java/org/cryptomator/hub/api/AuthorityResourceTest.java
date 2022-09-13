package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.sql.SQLException;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
@DisplayName("Resource /authorities")
public class AuthorityResourceTest {

	@Inject
	AgroalDataSource dataSource;

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
		@DisplayName("GET /search?query=Name 3000 returns 200 with \"user3000\", \"group3000\"")
		public void testGetSameUserGroupName() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('user3000', 'USER', 'Name 3000');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('user3000');
							
						""");

				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('group3000', 'GROUP', 'Name 3000');
							
						INSERT INTO "group_details" ("id")
						VALUES
							('group3000');
							
						""");
			}

			when().get("/authorities/search?query=Name 3000")
					.then().statusCode(200)
					.body("id", hasItems("user3000", "group3000"));
		}
	}
}