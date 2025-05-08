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
			return Map.of("hub.managed-instance", "true");
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