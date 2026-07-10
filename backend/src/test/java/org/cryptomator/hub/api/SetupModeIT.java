package org.cryptomator.hub.api;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.license.UnconfiguredLicense;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
		Mockito.doReturn(true).when(licenseHolder).isSetupRequired();
		Mockito.doReturn(UnconfiguredLicense.create("42")).when(licenseHolder).get();
		Mockito.doReturn(HubLicenseEntitlements.create()).when(licenseHolder).getEntitlements();
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
		@DisplayName("GET /billing returns 200 with the placeholder license's hub ID")
		void testGetBilling() {
			when().get("/billing")
					.then().statusCode(200)
					.body("hubId", Matchers.is("42"))
					.body("licensedSeats", Matchers.is(0));
		}

		@Test
		@DisplayName("POST /license/trial returns 204")
		void testRequestTrial() throws LicenseHolder.TrialLicenseRequestFailedException {
			when().post("/license/trial")
					.then().statusCode(204);

			Mockito.verify(licenseHolder).requestTrialLicense();
		}
	}

	@Nested
	@DisplayName("As unauthenticated user")
	class AsAnonymous {

		@Test
		@DisplayName("GET /config returns 200 with licenseSetupRequired=true")
		void testGetConfig() {
			when().get("/config")
					.then().statusCode(200)
					.body("licenseSetupRequired", Matchers.is(true));
		}

		@Test
		@DisplayName("GET /config returns licenseSetupRequired=false once a license is configured")
		void testGetConfigWithConfiguredLicense() {
			Mockito.doReturn(false).when(licenseHolder).isSetupRequired();

			when().get("/config")
					.then().statusCode(200)
					.body("licenseSetupRequired", Matchers.is(false));
		}

		@Test
		@DisplayName("non-allowlisted endpoints still require authentication first (401, not 402)")
		void testGatedEndpoint() {
			when().get("/vaults/accessible")
					.then().statusCode(401);
		}
	}

}
