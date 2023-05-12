package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.comparesEqualTo;

@QuarkusTest
@DisplayName("Resource /auditlog")
public class AuditLogResourceTest {

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	@TestSecurity(user = "Admin", roles = {"admin"})
	@DisplayName("As admin, GET /auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10 returns 200 with two entries")
	public void testGetAuditLogEntriesAdmin() {
		when().get("/auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder(comparesEqualTo(1000), comparesEqualTo(1111)));
	}

	@Test
	@TestSecurity(user = "User", roles = {"user"})
	@DisplayName("As user, GET /auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10 returns 403")
	public void testGetAuditLogEntriesUser() {
		when().get("/auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10")
				.then().statusCode(403);
	}
}
