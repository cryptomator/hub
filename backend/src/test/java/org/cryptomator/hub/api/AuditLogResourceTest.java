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
					INSERT INTO "audit_event" ("id", "timestamp", "type")
					VALUES
						('00000000-1111-1111-1111-aabbccddeeff', '1970-01-01T00:00:02.900Z', 'UNLOCK'),
						('00000000-2222-2222-2222-aabbccddeeff', '1970-01-01T00:00:03Z', 'UNLOCK'),
						('00000000-3333-3333-3333-aabbccddeeff', '1970-01-01T00:00:04Z', 'UNLOCK'),
						('00000000-4444-4444-4444-aabbccddeeff', '1970-01-01T00:00:05Z', 'UNLOCK');
					
					INSERT INTO "unlock_event" ("id", "user_id", "vault_id", "device_id", "result")
					VALUES
						('00000000-1111-1111-1111-aabbccddeeff', '11111111-0000-0000-0000-aabbccddeeff', '22222222-0000-0000-0000-aabbccddeeff', '33333333-0000-0000-0000-aabbccddeeff', 'SUCCESS'),
						('00000000-2222-2222-2222-aabbccddeeff', '11111111-0000-0000-0000-aabbccddeeff', '22222222-0000-0000-0000-aabbccddeeff', '33333333-0000-0000-0000-aabbccddeeff', 'SUCCESS'),
						('00000000-3333-3333-3333-aabbccddeeff', '11111111-0000-0000-0000-aabbccddeeff', '22222222-0000-0000-0000-aabbccddeeff', '33333333-0000-0000-0000-aabbccddeeff', 'DEVICE_NOT_AUTHORIZED'),
						('00000000-4444-4444-4444-aabbccddeeff', '11111111-0000-0000-0000-aabbccddeeff', '22222222-0000-0000-0000-aabbccddeeff', '33333333-0000-0000-0000-aabbccddeeff', 'SUCCESS');
					""");
		}
		when().get("/auditlog?startDate=3000&endDate=5000")
				.then().statusCode(200)
				.body("id", Matchers.containsInAnyOrder("00000000-2222-2222-2222-aabbccddeeff", "00000000-3333-3333-3333-aabbccddeeff"));
	}
}
