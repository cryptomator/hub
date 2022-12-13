package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.sql.SQLException;

import static io.restassured.RestAssured.when;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
@DisplayName("Resource /auditlog")
public class AuditLogResourceTest {

	@Inject
	AgroalDataSource dataSource;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	@TestSecurity(user = "Admin", roles = {"admin"})
	@DisplayName("GET /auditlog?startDate=3000&endDate=5000 returns 200 with two entries")
	public void testGetAuditLogEntries() throws SQLException {
		try (var s = dataSource.getConnection().createStatement()) {
			s.execute("""
					INSERT INTO "audit_log" ("id", "timestamp", "message")
					VALUES
						('id1', '1970-01-01T00:00:02.900Z', 'before period'),
						('id2', '1970-01-01T00:00:03Z', 'entry with start date'),
						('id3', '1970-01-01T00:00:04Z', 'entry within period'),
						('id4', '1970-01-01T00:00:05Z', 'entry with end date');
					""");
		}
		when().get("/auditlog?startDate=3000&endDate=5000")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder("id2", "id3"));
	}
}
