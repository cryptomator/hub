package org.cryptomator.hub.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Date;

import static io.restassured.RestAssured.when;

@QuarkusTest
@DisplayName("API in setup mode (no license configured)")
class SetupModeIT {

	@InjectMock
	LicenseHolder licenseHolder;

	@InjectMock
	OidcConfigurationMetadata oidcConfigurationMetadata; // not available in %test, but required by ConfigResource

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	void setup() {
		var licenseToken = Mockito.mock(DecodedJWT.class);
		var seatsClaim = Mockito.mock(com.auth0.jwt.interfaces.Claim.class);
		Mockito.doReturn(true).when(licenseHolder).isSetupRequired();
		Mockito.doReturn(licenseToken).when(licenseHolder).get();
		Mockito.doReturn(HubLicenseEntitlements.create()).when(licenseHolder).getEntitlements();
		Mockito.doReturn("42").when(licenseToken).getId();
		Mockito.doReturn("unconfigured@localhost").when(licenseToken).getSubject();
		Mockito.doReturn(seatsClaim).when(licenseToken).getClaim("seats");
		Mockito.doReturn(0).when(seatsClaim).asInt();
		Mockito.doReturn(Date.from(Instant.EPOCH)).when(licenseToken).getIssuedAt();
		Mockito.doReturn(Date.from(Instant.parse("3000-01-01T00:00:00Z"))).when(licenseToken).getExpiresAt();
		Mockito.doReturn("token").when(licenseToken).getToken();
		Mockito.doReturn("http://localhost:8180/auth").when(oidcConfigurationMetadata).getAuthorizationUri();
		Mockito.doReturn("http://localhost:8180/token").when(oidcConfigurationMetadata).getTokenUri();
	}

	@Nested
	@DisplayName("As user")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	class AsUser {

		@Test
		@DisplayName("non-allowlisted endpoints return 402")
		void testGatedEndpoints() {
			when().get("/vaults/accessible")
					.then().statusCode(402);
			when().get("/license/user-info")
					.then().statusCode(402);
		}

		@Test
		@DisplayName("GET /users/me returns 200")
		void testGetMe() {
			when().get("/users/me")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("PUT /users/me returns 201")
		void testPutMe() {
			when().put("/users/me")
					.then().statusCode(201);
		}
	}

	@Nested
	@DisplayName("As admin")
	@TestSecurity(user = "Admin", roles = {"admin", "user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	class AsAdmin {

		@Test
		@DisplayName("GET /billing returns 200")
		void testGetBilling() {
			when().get("/billing")
					.then().statusCode(200)
					.body("hubId", Matchers.is("42"))
					.body("licensedSeats", Matchers.is(0));
		}
	}

	@Nested
	@DisplayName("As unauthenticated user")
	class AsAnonymous {

		@Test
		@DisplayName("GET /config returns 200")
		void testGetConfig() {
			when().get("/config")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("non-allowlisted endpoints still require authentication first (401, not 402)")
		void testGatedEndpoint() {
			when().get("/vaults/accessible")
					.then().statusCode(401);
		}
	}

}
