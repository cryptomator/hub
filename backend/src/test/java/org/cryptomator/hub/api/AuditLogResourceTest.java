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
	@DisplayName("As admin, GET /auditlog?startDate=2020-02-20T00:00:00.000Z&endDate=2020-02-20T23:59:59.999Z&paginationId=9999 returns 200 with 20 entries")
	public void testGetAuditLogEntries() {
		given().param("startDate", "2020-02-20T00:00:00.000Z")
				.param("endDate", "2020-02-20T23:59:59.999Z")
				.param("paginationId", 9999L)
				.when().get("/auditlog")
				.then().statusCode(200)
				.body("id", Matchers.containsInRelativeOrder(comparesEqualTo(4242), comparesEqualTo(3000), comparesEqualTo(2003), comparesEqualTo(2002), comparesEqualTo(2001), comparesEqualTo(2000), comparesEqualTo(1111), comparesEqualTo(201), comparesEqualTo(200), comparesEqualTo(102), comparesEqualTo(101), comparesEqualTo(100), comparesEqualTo(31), comparesEqualTo(30), comparesEqualTo(23), comparesEqualTo(22), comparesEqualTo(21), comparesEqualTo(20), comparesEqualTo(12), comparesEqualTo(11)));
	}

	@Test
	@TestSecurity(user = "Admin", roles = {"admin"})
	@DisplayName("As admin, GET /auditlog?startDate=2020-02-20T00:00:00.000Z&endDate=2020-02-20T23:59:59.999Z&pageSize=10&paginationId=1000&order=asc returns 200 with 3 entries")
	public void testGetAuditLogEntriesPageSizeAsc() {
		given().param("startDate", "2020-02-20T00:00:00.000Z")
				.param("endDate", "2020-02-20T23:59:59.999Z")
				.param("pageSize", 3)
				.param("paginationId", 1000L)
				.param("order", "asc")
				.when().get("/auditlog")
				.then().statusCode(200)
				.body("id", Matchers.containsInRelativeOrder(comparesEqualTo(1111), comparesEqualTo(2000), comparesEqualTo(2001)));
	}

	@Test
	@TestSecurity(user = "User", roles = {"user"})
	@DisplayName("As user, GET /auditlog?startDate=2020-02-20T00:00:00.000Z&endDate=2020-02-20T23:59:59.999Z&pageSize=10 returns 403")
	public void testGetAuditLogEntriesAsUser() {
		when().get("/auditlog?startDate=2020-02-20T00:00:00.000Z&endDate=2020-02-20T23:59:59.999ZZ&pageSize=10")
				.then().statusCode(403);
	}

}
