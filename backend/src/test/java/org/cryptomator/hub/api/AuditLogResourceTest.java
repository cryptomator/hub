package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalToIgnoringCase;

@QuarkusTest
@DisplayName("Resource /auditlog")
public class AuditLogResourceTest {

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	@TestSecurity(user = "Admin", roles = {"admin"})
	@DisplayName("GET /auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z returns 200 with two entries")
	public void testGetAuditLogEntries() {
		when().get("/auditlog?startDate=1970-01-01T00:00:03Z&endDate=1970-01-01T00:00:05Z")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder(equalToIgnoringCase("7E57C0DE-0000-4000-8000-000510002222"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-000510003333")));
	}
}
