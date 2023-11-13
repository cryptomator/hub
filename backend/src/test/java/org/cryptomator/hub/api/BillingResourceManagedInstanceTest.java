package org.cryptomator.hub.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
@DisplayName("Resource /billing managed instance")
@TestSecurity(user = "Admin", roles = {"admin"})
@OidcSecurity(claims = {
		@Claim(key = "sub", value = "admin")
})
@TestProfile(BillingResourceManagedInstanceTest.ManagedInstanceTestProfile.class)
public class BillingResourceManagedInstanceTest {

	@Inject
	AgroalDataSource dataSource;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	public static class ManagedInstanceTestProfile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of("hub.managed-instance", "true");
		}
	}

	@Test
	@DisplayName("GET /billing returns 401 with empty license managed instance")
	public void testGetEmptyManagedInstance() throws SQLException {
		try (var c = dataSource.getConnection(); var s = c.createStatement()) {
			s.execute("""
					UPDATE "settings"
					SET "hub_id" = '42', "license_key" = null
					WHERE "id" = 0;
					""");
		}

		when().get("/billing")
				.then().statusCode(200)
				.body("hubId", is("42"))
				.body("hasLicense", is(false))
				.body("email", nullValue())
				.body("totalSeats", is(0))
				.body("remainingSeats", is(0))
				.body("issuedAt", nullValue())
				.body("expiresAt", nullValue());
	}
}