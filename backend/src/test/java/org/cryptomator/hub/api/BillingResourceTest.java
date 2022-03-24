package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
@DisplayName("Resource /billing")
public class BillingResourceTest {

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As admin")
	@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"}, clean = false)
	@TestSecurity(user = "Admin", roles = {"admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class AsAdmin {

		private static final String INITIAL_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm";
		private static final String UPDATED_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA1MDIxMCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AfYO7yp2-HpfT3yAfhX-2Hc-y4POPRevUNhW9IrL0Ru4BuwJCdf7KLuYIQFeNQ8Etz2RIg6rAnoVJ-xuP4RNE7RmAJuVhrYOyxHmF6btj3e3ES9JyBJW15Yw4R9iM3LKycNxL5OXsktC7I--_IOrB70C05511uGoifbKvvrnbapfn8gE";

		@Test
		@Order(1)
		@DisplayName("GET /billing returns 200")
		public void testGet() {
			when().get("/billing")
					.then().statusCode(200)
					.body("hub_id", is("42"))
					.body("token", is(INITIAL_TOKEN));
		}

		@Test
		@Order(2)
		@DisplayName("PUT /billing/token returns 204")
		public void testPutValidToken() {
			given().contentType(ContentType.TEXT).body(UPDATED_TOKEN)
					.when().put("/billing/token")
					.then().statusCode(204);
		}

		@Test
		@Order(3)
		@DisplayName("GET /billing returns 200 with updated token")
		public void testGetUpdated() {
			when().get("/billing")
					.then().statusCode(200)
					.body("hub_id", is("42"))
					.body("token", is(UPDATED_TOKEN));
		}

		@Test
		@Order(4)
		@DisplayName("PUT /billing/token returns 400 due to expired token")
		public void testPutExpiredToken() {
			given().contentType(ContentType.TEXT).body("eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA1MTA0MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6LTYyMTY3MjE5MjAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AK9Du5MNsyVgOzicgi2S7ECxxqZPBLP8cFByAAZ7_y96NEvrwOiR8NNmlZlfvebMfckaYUEg-nf3BAd1JHAxur1UADyYbKSLoMs4B69SkbKW0drjfY9RjFUhO_w6sS4gg39_X_IhbwW6wRkGFoqGRI0juaCPViQqV5WFIcj7RbuCcNJ6")
					.when().put("/billing/token")
					.then().statusCode(400);
		}

		@Test
		@Order(5)
		@DisplayName("PUT /billing/token returns 400 due to invalid signature")
		public void testPutTokenWithInvalidSignature() {
			given().contentType(ContentType.TEXT).body("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
					.when().put("/billing/token")
					.then().statusCode(400);
		}

		@Test
		@Order(6)
		@DisplayName("PUT /billing/token returns 400 due to malformed token")
		public void testPutMalformedToken() {
			given().contentType(ContentType.TEXT).body("hello world")
					.when().put("/billing/token")
					.then().statusCode(400);
		}

	}

	@Nested
	@DisplayName("As any other role")
	@TestSecurity(user = "User Name 1", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsAnyOtherRole {

		@DisplayName("403 Forbidden")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /billing"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(403);
		}

		@DisplayName("403 Forbidden")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"PUT, /billing/token"
		})
		public void testPut(String method, String path) {
			given().contentType(ContentType.TEXT).body("")
					.when().request(method, path)
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /billing"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"PUT, /billing/token"
		})
		public void testPut(String method, String path) {
			given().contentType(ContentType.TEXT).body("")
					.when().request(method, path)
					.then().statusCode(401);
		}

	}
}