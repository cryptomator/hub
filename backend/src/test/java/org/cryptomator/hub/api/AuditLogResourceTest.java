package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
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
	@DisplayName("As admin, GET /auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10&paginationId=9999 returns 200 with three entries")
	public void testGetAuditLogEntriesAdmin() {
		given().param("startDate", "1970-01-01T00:00:03Z")
				.param("endDate", "1970-01-01T00:00:05Z")
				.param("pageSize", 10)
				.param("paginationId", 9999L)
				.when().get("/auditlog")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder(comparesEqualTo(1000), comparesEqualTo(1001), comparesEqualTo(1111)));
	}

	@Test
	@TestSecurity(user = "Admin", roles = {"admin"})
	@DisplayName("As admin, GET /auditlog?startDate=1970-01-01T00:00:02.900Z&endDate=1970-01-01T00:00:05.100Z&pageSize=10&paginationId=9999 returns 200 with two entries")
	public void testGetAuditLogEntriesAsc() {
		given().param("startDate", "1970-01-01T00:00:02.900Z")
				.param("endDate", "1970-01-01T00:00:05.100Z")
				.param("pageSize", 3)
				.param("paginationId", 1001L)
				.param("order", "asc")
				.when().get("/auditlog")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder(comparesEqualTo(4242), comparesEqualTo(1111)));
	}

	@Test
	@TestSecurity(user = "User", roles = {"user"})
	@DisplayName("As user, GET /auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10 returns 403")
	public void testGetAuditLogEntriesUser() {
		when().get("/auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z&pageSize=10")
				.then().statusCode(403);
	}
}
