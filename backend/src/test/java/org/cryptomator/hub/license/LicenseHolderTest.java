package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.InternalServerErrorException;
import org.cryptomator.hub.entities.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LicenseHolderTest {

	Settings.Repository settingsRepo = mock(Settings.Repository.class);
	RandomSleeper randomSleeper = mock(RandomSleeper.class);
	LicenseValidator validator = mock(LicenseValidator.class);
	LicenseApi licenseApi = mock(LicenseApi.class);

	LicenseHolder licenseHolder;

	@BeforeEach
	void resetTestclass() {
		licenseHolder = buildLicenseHolder(Optional.empty(), Optional.empty());
	}

	private LicenseHolder buildLicenseHolder(Optional<String> initialId, Optional<String> initialLicenseToken) {
		return new LicenseHolder(false, initialId, initialLicenseToken, Optional.empty(), Optional.empty(), validator, randomSleeper, settingsRepo, licenseApi);
	}

	// mocks a license whose entitlements claim is parseable, which happens eagerly whenever a license is installed
	private static DecodedJWT mockLicense() {
		var entitlementsClaim = mock(Claim.class);
		doReturn(HubLicenseEntitlements.create()).when(entitlementsClaim).as(HubLicenseEntitlements.class);
		var license = mock(DecodedJWT.class);
		doReturn(entitlementsClaim).when(license).getClaim("org.cryptomator.hub.entitlements");
		return license;
	}

	@Nested
	@DisplayName("Testing loadLicense()")
	class TestLoadLicense {

		private Settings settings;
		private LicenseHolder licenseHolderSpy;

		@BeforeEach
		void setup() {
			settings = mock(Settings.class);
			licenseHolderSpy = Mockito.spy(licenseHolder);
			Mockito.doReturn(settings).when(settingsRepo).get();
		}

		@Test
		@DisplayName("call validateExistingLicense(), if DB contains existing token")
		void testValidateExistingLicense() {
			//to show check, that db has higher precedence
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.of("43"), Optional.of("initToken")));
			Mockito.doReturn("token").when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			var license = mockLicense();
			Mockito.doReturn(license).when(licenseHolderSpy).validateExistingLicense(any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(license, result.license());
			Assertions.assertFalse(result.unconfigured());
			verify(licenseHolderSpy).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(any(), any(), any());
		}

		@Test
		@DisplayName("fall back to unconfigured license, if DB token is invalid")
		void testInvalidExistingLicense() {
			Mockito.doReturn("token").when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolderSpy).validateExistingLicense(any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertTrue(result.unconfigured());
			Assertions.assertEquals("42", result.license().getId());
			Assertions.assertEquals(0L, result.entitlements().seats());
			verify(settings, never()).setLicenseKey(any()); // stored license key remains untouched
			verify(settings, never()).setHubId(any());
		}

		@Test
		@DisplayName("call validateAndApplyInitLicense(), if DB doesn't contain token but init config does")
		void testApplyInitLicense() {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.of("43"), Optional.of("token")));
			Mockito.doReturn(null).when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			var license = mockLicense();
			Mockito.doReturn(license).when(licenseHolderSpy).validateAndApplyInitLicense(any(), any(), any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(license, result.license());
			Assertions.assertFalse(result.unconfigured());
			verify(licenseHolderSpy, never()).validateExistingLicense(any());
			verify(licenseHolderSpy).validateAndApplyInitLicense(settings, "token", "43");
		}

		@Test
		@DisplayName("fall back to unconfigured license, if init token is invalid")
		void testInvalidInitLicense() {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.of("43"), Optional.of("token")));
			Mockito.doReturn(null).when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolderSpy).validateAndApplyInitLicense(any(), any(), any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertTrue(result.unconfigured());
			verify(settings, never()).setLicenseKey(any());
			verify(settings, never()).setHubId(any());
		}

		@DisplayName("fall back to unconfigured license without contacting the license server, if neither DB nor init config contains a token")
		@ParameterizedTest
		@CsvSource(value = {
				"null, null",
				"null, 43",
				"initToken, null"
		}, nullValues = {"null"})
		void testUnconfiguredLicense(String initToken, String initId) {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.ofNullable(initId), Optional.ofNullable(initToken)));
			Mockito.doReturn(null).when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertTrue(result.unconfigured());
			verify(licenseHolderSpy, never()).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(Mockito.eq(settings), any(), any());
			verify(licenseApi, never()).generateChallenge();
		}

		@Test
		@DisplayName("init() enters setup mode without license refresh, if nothing is configured")
		void testInitInSetupMode() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn(null).when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();

			licenseHolderSpy.init();

			Assertions.assertTrue(licenseHolderSpy.isSetupRequired());
			Assertions.assertFalse(licenseHolderSpy.isExpired()); // dummy expires in year 3000
			verify(licenseHolderSpy, never()).refreshLicense();
		}
	}

	@Nested
	@DisplayName("Testing set(token, hubId)")
	class TestSetWithHubId {

		private Settings settings;
		private LicenseHolder licenseHolderSpy;

		@BeforeEach
		void setup() {
			settings = mock(Settings.class);
			licenseHolderSpy = Mockito.spy(licenseHolder);
			Mockito.doReturn(settings).when(settingsRepo).get();
			licenseHolderSpy.init(); // nothing configured -> setup mode
		}

		@Test
		@DisplayName("Setting a valid trial token adopts the hub ID, persists both and leaves setup mode")
		void testSetValidToken() {
			var decodedJWT = mockLicense();
			Mockito.doReturn(decodedJWT).when(validator).validate("token", "new-hub-id");

			licenseHolderSpy.set("token", "new-hub-id");

			verify(settings).setHubId("new-hub-id");
			verify(settings).setLicenseKey("token");
			verify(settingsRepo).persistAndFlush(settings);
			Assertions.assertFalse(licenseHolderSpy.isSetupRequired());
			Assertions.assertEquals(decodedJWT, licenseHolderSpy.get());
		}

		@Test
		@DisplayName("Setting an invalid token fails with exception and stays in setup mode")
		void testSetInvalidToken() {
			Mockito.doThrow(new JWTVerificationException("invalid")).when(validator).validate("token", "new-hub-id");

			Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolderSpy.set("token", "new-hub-id"));

			verify(settings, never()).setHubId(any());
			verify(settings, never()).setLicenseKey(any());
			verify(settingsRepo, never()).persistAndFlush(any());
			Assertions.assertTrue(licenseHolderSpy.isSetupRequired());
		}

		@Test
		@DisplayName("Throws IllegalStateException, if a license is already configured")
		void testAlreadyConfigured() {
			var decodedJWT = mockLicense();
			Mockito.doReturn(decodedJWT).when(validator).validate("token", "new-hub-id");
			licenseHolderSpy.set("token", "new-hub-id");

			Assertions.assertThrows(IllegalStateException.class, () -> licenseHolderSpy.set("token2", "other-id"));

			verify(validator, never()).validate("token2", "other-id");
		}
	}

	@Nested
	@DisplayName("Testing refresh guards in setup mode")
	class TestSetupModeRefreshGuards {

		private LicenseHolder licenseHolderSpy;

		@BeforeEach
		void setup() {
			var settings = mock(Settings.class);
			licenseHolderSpy = Mockito.spy(licenseHolder);
			Mockito.doReturn(settings).when(settingsRepo).get();
			licenseHolderSpy.init(); // nothing configured -> setup mode
		}

		@Test
		@DisplayName("scheduled license refresh is skipped in setup mode")
		void testScheduledRefreshSkipped() throws InterruptedException, LicenseHolder.LicenseRefreshFailedException {
			licenseHolderSpy.scheduleLicenseRefresh();

			verify(randomSleeper, never()).sleep(anyInt(), anyInt(), any());
			verify(licenseHolderSpy, never()).refreshLicense();
		}

		@Test
		@DisplayName("refreshLicense() throws LicenseRefreshFailedException in setup mode")
		void testRefreshLicenseThrows() throws LicenseHolder.LicenseRefreshFailedException {
			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any());
		}
	}

	@Test
	@DisplayName("Valid db token does not change settings")
	void testValidateExistingSuccess() {
		Settings settings = mock(Settings.class);
		when(settings.getLicenseKey()).thenReturn("token");
		when(settings.getHubId()).thenReturn("42");
		when(settingsRepo.get()).thenReturn(settings);
		when(validator.validate("token", "42")).thenReturn(Mockito.mock(DecodedJWT.class));

		licenseHolder.validateExistingLicense(settings);

		verify(settings, never()).setHubId(any());
		verify(settings, never()).setLicenseKey(any());
	}

	@Test
	@DisplayName("Invalid db token fails validation")
	void testValidateExistingFailure() {
		Settings settings = mock(Settings.class);
		when(settings.getLicenseKey()).thenReturn("token");
		when(settings.getHubId()).thenReturn("42");
		when(settingsRepo.get()).thenReturn(settings);
		Mockito.doThrow(new JWTVerificationException("invalid")).when(validator).validate("token", "42");

		Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.validateExistingLicense(settings));
	}

	@Test
	@DisplayName("Valid init token is persisted with hubID to db")
	void testApplyInitSuccess() {
		Settings settings = mock(Settings.class);
		when(validator.validate("token", "42")).thenReturn(Mockito.mock(DecodedJWT.class));

		licenseHolder.validateAndApplyInitLicense(settings, "token", "42");

		verify(settings).setHubId("42");
		verify(settings).setLicenseKey("token");
		verify(settingsRepo).persistAndFlush(settings);
	}

	@Test
	@DisplayName("Invalid init token does not change settings")
	void testApplyInitFailure() {
		Settings settings = mock(Settings.class);
		when(settings.getLicenseKey()).thenReturn("token");
		when(settings.getHubId()).thenReturn("42");
		when(settingsRepo.get()).thenReturn(settings);
		Mockito.doThrow(new JWTVerificationException("invalid")).when(validator).validate("token", "42");

		Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.validateAndApplyInitLicense(settings, "token", "42"));

		verify(settings, never()).setHubId(any());
		verify(settings, never()).setLicenseKey(any());
	}

	@Nested
	@DisplayName("Testing  set() method")
	class TestSetter {

		@BeforeEach
		void setup() throws InterruptedException {
			Mockito.doNothing().when(randomSleeper).sleep(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
		}

		@Test
		@DisplayName("Setting a valid token validates and persists it to db")
		void testSetValidToken() {
			var decodedJWT = mockLicense();
			when(validator.validate("token", "42")).thenReturn(decodedJWT);

			Settings settings = mock(Settings.class);
			when(settings.getHubId()).thenReturn("42");
			when(settingsRepo.get()).thenReturn(settings);

			licenseHolder.set("token");

			verify(validator).validate("token", "42");
			verify(settings).setLicenseKey("token");
			verify(settingsRepo).persistAndFlush(settings);
			Assertions.assertEquals(decodedJWT, licenseHolder.get()); //TODO: not very unit test like
		}

		@Test
		@DisplayName("Setting an invalid token fails with exception")
		void testSetInvalidToken() {
			Settings settings = mock(Settings.class);
			Mockito.doReturn(settings).when(settingsRepo).get();
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doThrow(new JWTVerificationException("")).when(validator).validate("token", "42");

			Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.set("token"));

			verify(validator).validate("token", "42");
			verify(settingsRepo, never()).persist((Settings) any());
		}
	}

	@Nested
	@DisplayName("Testing refreshLicense()")
	class RefreshLicense {

		private LicenseHolder licenseHolderSpy;
		private DecodedJWT licenseJwt;

		@BeforeEach
		void setup() {
			licenseHolderSpy = Mockito.spy(licenseHolder);
			licenseJwt = mock(DecodedJWT.class);

			Mockito.doReturn("token").when(licenseJwt).getToken();
			Mockito.doReturn(licenseJwt).when(licenseHolderSpy).get();
		}

		@Test
		@DisplayName("If license request throws, do not set license")
		void testRefreshLicenseFailingRequest() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doThrow(new LicenseHolder.LicenseRefreshFailedException("http error 500")).when(licenseHolderSpy).requestLicenseRefresh(eq("token"));

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy).requestLicenseRefresh(eq("token"));
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Successful refresh request, but failing validation")
		void testRefreshLicenseFailedValidation() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh("token");
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolderSpy).set("newToken");

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy).requestLicenseRefresh(eq("token"));
			verify(licenseHolderSpy).set("newToken");
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Successful refresh")
		void testRefreshLicenseSuccess() throws LicenseHolder.LicenseRefreshFailedException {
			var settings = Mockito.mock(Settings.class);
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doReturn(settings).when(settingsRepo).get();
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh("token");
			Mockito.doReturn(mockLicense()).when(validator).validate("newToken", "42");

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(eq("token"));
			verify(licenseHolderSpy).set("newToken");
			verify(validator).validate("newToken", "42");
			verify(settings).setLicenseKey("newToken");
			verify(settingsRepo).persistAndFlush(settings);
		}

	}

	@Nested
	@DisplayName("Testing refreshLicense(UUID session)")
	class RefreshLicenseWithSession {

		private static final UUID SESSION = UUID.fromString("11111111-2222-3333-4444-555555555555");

		@Test
		@DisplayName("Successful refresh validates and persists the token fetched for the session")
		void testRefreshLicenseWithSessionSuccess() throws LicenseHolder.LicenseRefreshFailedException {
			var settings = mock(Settings.class);
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doReturn(settings).when(settingsRepo).get();
			Mockito.doReturn("newToken").when(licenseApi).getLicense(SESSION);
			Mockito.doReturn(mockLicense()).when(validator).validate("newToken", "42");

			licenseHolder.refreshLicense(SESSION);

			verify(licenseApi).getLicense(SESSION);
			verify(validator).validate("newToken", "42");
			verify(settings).setLicenseKey("newToken");
			verify(settingsRepo).persistAndFlush(settings);
		}

		@Test
		@DisplayName("Fetched token failing validation throws LicenseRefreshFailedException and is not persisted")
		void testRefreshLicenseWithSessionFailedValidation() {
			var settings = mock(Settings.class);
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doReturn(settings).when(settingsRepo).get();
			Mockito.doReturn("newToken").when(licenseApi).getLicense(SESSION);
			Mockito.doThrow(new JWTVerificationException("invalid")).when(validator).validate("newToken", "42");

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolder.refreshLicense(SESSION));

			verify(licenseApi).getLicense(SESSION);
			verify(validator).validate("newToken", "42");
			verify(settings, never()).setLicenseKey(any());
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Failing license request propagates and does not touch validation or settings")
		void testRefreshLicenseWithSessionFailingRequest() {
			Mockito.doThrow(new InternalServerErrorException()).when(licenseApi).getLicense(SESSION);

			Assertions.assertThrows(InternalServerErrorException.class, () -> licenseHolder.refreshLicense(SESSION));

			verify(licenseApi).getLicense(SESSION);
			verify(validator, never()).validate(any(), any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}
	}

	@Nested
	@DisplayName("Testing requestLicenseRefresh()")
	class RequestLicenseRefresh {

		private LicenseHolder licenseHolderSpy;
		private LicenseApi.Solution solvedChallenge;

		@BeforeEach
		void setup() {
			licenseHolderSpy = Mockito.spy(licenseHolder);
			solvedChallenge = Mockito.mock(LicenseApi.Solution.class);
			Mockito.doReturn(solvedChallenge).when(licenseHolderSpy).solveChallenge();
			Mockito.doReturn("fooBar123").when(solvedChallenge).toCaptcha();
		}

		@Test
		@DisplayName("successful refresh returns the new token")
		void testSuccess() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn("newToken").when(licenseApi).refreshLicense("token", "fooBar123");

			var result = licenseHolderSpy.requestLicenseRefresh("token");

			Assertions.assertEquals("newToken", result);
		}

		@Test
		@DisplayName("refresh endpoint error is converted to LicenseRefreshFailedException")
		void testUpstreamFailure() {
			Mockito.doThrow(new InternalServerErrorException()).when(licenseApi).refreshLicense("token", "fooBar123");

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolderSpy.requestLicenseRefresh("token"));
		}
	}

}
