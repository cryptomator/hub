package org.cryptomator.hub.api;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

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

	}

	@Nested
	@DisplayName("As any other role")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	class AsAnyOtherRole {

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

	}

	@Nested
	@DisplayName("As unauthenticated user")
	class AsAnonymous {

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

	}

}
