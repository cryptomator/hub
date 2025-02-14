package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Resource /billing managed instance")
@TestSecurity(user = "Admin", roles = {"admin"})
@OidcSecurity(claims = {
		@Claim(key = "sub", value = "admin")
})
@TestProfile(BillingResourceManagedInstanceIT.ManagedInstanceTestProfile.class)
public class BillingResourceManagedInstanceIT {

	@Inject
	LicenseHolder licenseHolder;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	public void setup() {
		licenseHolder.ensureLicenseExists();
	}

	public static class ManagedInstanceTestProfile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
					"hub.managed-instance", "true",
					"hub.initial-id", "42",
					"hub.initial-license", "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm"
					);
		}
	}

	@Test
	@DisplayName("GET /billing returns 200 billing data with managedInstance=true")
	public void testGetInitial() {
		when().get("/billing")
				.then().statusCode(200)
				.body("hubId", is("42"))
				.body("hasLicense", is(true))
				.body("email", is("hub@cryptomator.org"))
				.body("licensedSeats", is(5))
				.body("usedSeats", is(2))
				.body("issuedAt", is("2022-03-23T15:29:20Z"))
				.body("expiresAt", is("9999-12-31T00:00:00Z"))
				.body("managedInstance", is(true));
	}
}