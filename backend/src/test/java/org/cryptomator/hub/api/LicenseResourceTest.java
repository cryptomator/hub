package org.cryptomator.hub.api;

import com.auth0.jwt.exceptions.JWTVerificationException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@DisplayName("Resource /license")
class LicenseResourceTest {

	private static final UUID SESSION = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@InjectMock
	LicenseHolder licenseHolder;

	@InjectMock
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As admin")
	@TestSecurity(user = "Admin", roles = {"admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "admin")
	})
	class AsAdmin {

		@Test
		@DisplayName("PUT /license/trial returns 204 and installs the trial license along with its hub ID")
		void testInstallTrial() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.JSON).body(Map.of("hubId", "1337", "licenseKey", "a.b.c"))
					.when().put("/license/trial")
					.then().statusCode(204);

			Mockito.verify(licenseHolder).set("a.b.c", "1337");
		}

		@Test
		@DisplayName("PUT /license/trial returns 400 if the license is invalid")
		void testInstallTrialInvalidToken() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolder).set("a.b.c", "1337");

			given().contentType(ContentType.JSON).body(Map.of("hubId", "1337", "licenseKey", "a.b.c"))
					.when().put("/license/trial")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /license/trial returns 409 if a license is already configured")
		void testInstallTrialConflict() {
			Mockito.doReturn(false).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.JSON).body(Map.of("hubId", "1337", "licenseKey", "a.b.c"))
					.when().put("/license/trial")
					.then().statusCode(409);

			Mockito.verify(licenseHolder, Mockito.never()).set(Mockito.anyString(), Mockito.anyString());
		}

		@Test
		@DisplayName("POST /license/refresh returns 204 and refreshes from the refreshUrl")
		void testRefresh() throws LicenseHolder.LicenseRefreshFailedException {
			when().post("/license/refresh")
					.then().statusCode(204);

			Mockito.verify(licenseHolder).refreshLicense();
		}

		@Test
		@DisplayName("POST /license/refresh returns 500 if the refresh fails")
		void testRefreshFailure() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doThrow(LicenseHolder.LicenseRefreshFailedException.class).when(licenseHolder).refreshLicense();

			when().post("/license/refresh")
					.then().statusCode(500);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 204 and refreshes for that session")
		void testRefreshSession() throws LicenseHolder.LicenseRefreshFailedException {
			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(204);

			Mockito.verify(licenseHolder).refreshLicense(SESSION);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 500 if the refresh fails")
		void testRefreshSessionFailure() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doThrow(LicenseHolder.LicenseRefreshFailedException.class).when(licenseHolder).refreshLicense(SESSION);

			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(500);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 204 during setup mode (store callback on the setup page)")
		void testRefreshSessionDuringSetup() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(204);

			Mockito.verify(licenseHolder).refreshLicense(SESSION);
		}

		@Test
		@DisplayName("POST /license/refresh with unknown session returns 404")
		void testRefreshSessionUnknown() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doThrow(new NotFoundException()).when(licenseHolder).refreshLicense(SESSION);

			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(404);
		}

	}

	@Nested
	@DisplayName("As user")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	class AsUser {

		@Test
		@DisplayName("GET /license/user-info returns 200 with seats, expiry and grace period end")
		void testGetUserInfo() {
			Mockito.doReturn(Instant.parse("2026-07-27T00:00:00Z")).when(licenseHolder).getExpiresAt();
			Mockito.doReturn(Instant.parse("2026-08-05T00:00:00Z")).when(licenseHolder).getGracePeriodEndsAt();
			Mockito.doReturn(HubLicenseEntitlements.create().withSeats(5)).when(licenseHolder).getEntitlements();
			Mockito.doReturn(3L).when(effectiveVaultAccessRepo).countSeatOccupyingUsers();

			when().get("/license/user-info")
					.then().statusCode(200)
					.body("licensedSeats", is(5))
					.body("usedSeats", is(3))
					.body("expiresAt", is("2026-07-27T00:00:00Z"))
					.body("gracePeriodEndsAt", is("2026-08-05T00:00:00Z"));
		}

		@ParameterizedTest(name = "showTrialHint={0}, emergencyAccessEnabled={1}, keycloakAccessEnabled={2}")
		@DisplayName("GET /license/entitlements returns 200 with all entitlements, including nested client licenses")
		@CsvSource({"true,false,false", "false,true,false", "false,false,true"}) // exactly one flag set per run, so swapping any two flags in the DTO mapping fails
		void testGetEntitlements(boolean showTrialHint, boolean emergencyAccessEnabled, boolean keycloakAccessEnabled) {
			var entitlements = HubLicenseEntitlements.create()
					.withSeats(5)
					.withShowTrialHint(showTrialHint)
					.withAuditLogRetentionDays(30)
					.withEmergencyAccessEnabled(emergencyAccessEnabled)
					.withKeycloakAccessEnabled(keycloakAccessEnabled)
					.withIosLicense("ios.jwt")
					.withAndroidLicense("android.jwt")
					.withDesktopLicense("desktop.jwt");
			Mockito.doReturn(entitlements).when(licenseHolder).getEntitlements();

			when().get("/license/entitlements")
					.then().statusCode(200)
					.body("seats", is(5))
					.body("showTrialHint", is(showTrialHint))
					.body("auditLogRetentionDays", is(30))
					.body("emergencyAccessEnabled", is(emergencyAccessEnabled))
					.body("keycloakAccessEnabled", is(keycloakAccessEnabled))
					.body("iosLicense", is("ios.jwt"))
					.body("androidLicense", is("android.jwt"))
					.body("desktopLicense", is("desktop.jwt"));
		}

	}

	@Nested
	@DisplayName("As any other role")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	class AsAnyOtherRole {

		@Test
		@DisplayName("PUT /license/trial returns 403 Forbidden")
		void testInstallTrial() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.JSON).body(Map.of("hubId", "1337", "licenseKey", "a.b.c"))
					.when().put("/license/trial")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("POST /license/refresh returns 403 Forbidden")
		void testRefresh() {
			when().post("/license/refresh")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 403 Forbidden")
		void testRefreshSession() {
			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 403 Forbidden also during setup mode")
		void testRefreshSessionDuringSetup() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As vault creator without the user role")
	@TestSecurity(user = "Vault Creator", roles = {"create-vaults"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "creator")
	})
	class AsVaultCreator {

		@Test
		@DisplayName("GET /license/entitlements returns 403 Forbidden")
		void testGetEntitlements() {
			when().get("/license/entitlements")
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	class AsAnonymous {

		@Test
		@DisplayName("GET /license/user-info returns 401 Unauthorized")
		void testGetUserInfo() {
			when().get("/license/user-info")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("GET /license/entitlements returns 401 Unauthorized")
		void testGetEntitlements() {
			when().get("/license/entitlements")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("PUT /license/trial returns 401 Unauthorized")
		void testInstallTrial() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.JSON).body(Map.of("hubId", "1337", "licenseKey", "a.b.c"))
					.when().put("/license/trial")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("POST /license/refresh returns 401 Unauthorized")
		void testRefresh() {
			when().post("/license/refresh")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 401 Unauthorized")
		void testRefreshSession() {
			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("POST /license/refresh with session returns 401 Unauthorized also during setup mode")
		void testRefreshSessionDuringSetup() {
			Mockito.doReturn(true).when(licenseHolder).isSetupRequired();

			given().contentType(ContentType.URLENC).formParam("session", SESSION.toString())
					.when().post("/license/refresh")
					.then().statusCode(401);
		}

	}

}
