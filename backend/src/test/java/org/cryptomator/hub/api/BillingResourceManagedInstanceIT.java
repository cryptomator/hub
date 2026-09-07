package org.cryptomator.hub.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Date;
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

	@InjectMock
	LicenseHolder licenseHolder;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	public void setup() {
		var licenseToken = Mockito.mock(DecodedJWT.class);
		Mockito.doReturn(true).when(licenseHolder).isManagedInstance();
		Mockito.doReturn(licenseToken).when(licenseHolder).get();
		Mockito.doReturn("42").when(licenseToken).getId();
		Mockito.doReturn("hub@cryptomator.org").when(licenseToken).getSubject();
		var seatsClaim = Mockito.mock(com.auth0.jwt.interfaces.Claim.class);
		Mockito.doReturn(seatsClaim).when(licenseToken).getClaim("seats");
		Mockito.doReturn(5).when(seatsClaim).asInt();
		Mockito.doReturn(Date.from(Instant.parse("2022-03-23T15:29:20Z"))).when(licenseToken).getIssuedAt();
		Mockito.doReturn(Date.from(Instant.parse("9999-12-31T00:00:00Z"))).when(licenseToken).getExpiresAt();
		Mockito.doReturn("foo").when(licenseToken).getToken();
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
				.body("email", is("hub@cryptomator.org"))
				.body("licensedSeats", is(5))
				.body("usedSeats", is(2))
				.body("issuedAt", is("2022-03-23T15:29:20Z"))
				.body("expiresAt", is("9999-12-31T00:00:00Z"))
				.body("managedInstance", is(true));
	}
}