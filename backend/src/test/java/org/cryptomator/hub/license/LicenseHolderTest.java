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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LicenseHolderTest {

	Settings.Repository settingsRepo = mock(Settings.Repository.class);
	RandomSleeper randomSleeper = mock(RandomSleeper.class);
	LicenseValidator validator = mock(LicenseValidator.class);
	LicenseApi licenseApi = mock(LicenseApi.class);

	LicenseHolder licenseHolder;

	@BeforeEach
	public void resetTestclass() {
		licenseHolder = buildLicenseHolder(Optional.empty(), Optional.empty());
	}

	private LicenseHolder buildLicenseHolder(Optional<String> initialId, Optional<String> initialLicenseToken) {
		return new LicenseHolder(false, initialId, initialLicenseToken, Optional.empty(), Optional.empty(), validator, randomSleeper, settingsRepo, licenseApi);
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
			var license = Mockito.mock(DecodedJWT.class);
			Mockito.doReturn(license).when(licenseHolderSpy).validateExistingLicense(any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(license, result);
			verify(licenseHolderSpy).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(any(), any(), any());
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(settings);
		}

		@Test
		@DisplayName("fall back to unconfigured license, if DB token is invalid")
		void testInvalidExistingLicense() {
			Mockito.doReturn("token").when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolderSpy).validateExistingLicense(any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals("42", result.getId());
			Assertions.assertEquals(0L, result.getClaim("seats").asLong());
			verify(settings, never()).setLicenseKey(any()); // stored license key remains untouched
			verify(settings, never()).setHubId(any());
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(any());
		}

		@DisplayName("call validateAndApplyInitLicense(), if DB doesn't contain token but init config does")
		@ParameterizedTest
		@CsvSource(value = {
				"dbToken, null",
				"null, null",
				"null, 42"
		}, nullValues = {"null"})
		void testApplyInitLicense(String dbToken, String dbHubId) {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.of("43"), Optional.of("token")));
			Mockito.doReturn(dbToken).when(settings).getLicenseKey();
			Mockito.doReturn(dbHubId).when(settings).getHubId();
			var license = Mockito.mock(DecodedJWT.class);
			Mockito.doReturn(license).when(licenseHolderSpy).validateAndApplyInitLicense(any(), any(), any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(license, result);
			verify(licenseHolderSpy, never()).validateExistingLicense(any());
			verify(licenseHolderSpy).validateAndApplyInitLicense(settings, "token", "43");
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(settings);
		}

		@Test
		@DisplayName("fall back to unconfigured license, if init token is invalid")
		void testInvalidInitLicense() {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.of("43"), Optional.of("token")));
			Mockito.doReturn(null).when(settings).getLicenseKey();
			Mockito.doReturn("42").when(settings).getHubId();
			Mockito.doThrow(new JWTVerificationException("invalid")).when(licenseHolderSpy).validateAndApplyInitLicense(any(), any(), any());

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(0L, result.getClaim("seats").asLong());
			verify(settings, never()).setLicenseKey(any());
			verify(settings, never()).setHubId(any());
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(any());
		}

		@DisplayName("fall back to unconfigured license without contacting the license server, if neither DB nor init config contains a token")
		@ParameterizedTest
		@CsvSource(value = {
				"dbToken, null, null, 43",
				"null, 42, null, 43",
				"dbToken, null, initToken, null"
		}, nullValues = {"null"})
		void testUnconfiguredLicense(String dbToken, String dbHubId, String initToken, String initId) {
			licenseHolderSpy = Mockito.spy(buildLicenseHolder(Optional.ofNullable(initId), Optional.ofNullable(initToken)));
			Mockito.doReturn(dbToken).when(settings).getLicenseKey();
			Mockito.doReturn(dbHubId).when(settings).getHubId();

			var result = licenseHolderSpy.loadLicense();

			Assertions.assertEquals(0L, result.getClaim("seats").asLong());
			verify(licenseHolderSpy, never()).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(Mockito.eq(settings), any(), any());
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(settings);
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
	@DisplayName("Testing requestTrialLicense()")
	class TestRequestTrialLicense {

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
		@DisplayName("successful trial request installs the license and leaves setup mode")
		void testSuccess() throws LicenseHolder.TrialLicenseRequestFailedException {
			var trialLicense = mock(DecodedJWT.class);
			Mockito.doReturn(trialLicense).when(licenseHolderSpy).requestAnonTrialLicense(settings);

			licenseHolderSpy.requestTrialLicense();

			Assertions.assertFalse(licenseHolderSpy.isSetupRequired());
			Assertions.assertEquals(trialLicense, licenseHolderSpy.get());
		}

		@Test
		@DisplayName("failing trial request throws TrialLicenseRequestFailedException and stays in setup mode")
		void testUpstreamFailure() {
			Mockito.doThrow(new InternalServerErrorException()).when(licenseHolderSpy).requestAnonTrialLicense(settings);

			Assertions.assertThrows(LicenseHolder.TrialLicenseRequestFailedException.class, licenseHolderSpy::requestTrialLicense);

			Assertions.assertTrue(licenseHolderSpy.isSetupRequired());
		}

		@Test
		@DisplayName("throws IllegalStateException, if a license is already configured")
		void testAlreadyConfigured() throws LicenseHolder.TrialLicenseRequestFailedException {
			var trialLicense = mock(DecodedJWT.class);
			Mockito.doReturn(trialLicense).when(licenseHolderSpy).requestAnonTrialLicense(settings);
			licenseHolderSpy.requestTrialLicense();

			Assertions.assertThrows(IllegalStateException.class, licenseHolderSpy::requestTrialLicense);

			verify(licenseHolderSpy, times(1)).requestAnonTrialLicense(any());
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
			var decodedJWT = mock(DecodedJWT.class);
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
			var decodedJWT = mock(DecodedJWT.class);
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

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
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
		when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);

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
		when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);

		Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.validateAndApplyInitLicense(settings, "token", "42"));

		verify(settings, never()).setHubId(any());
		verify(settings, never()).setLicenseKey(any());
	}

	@Test
	@DisplayName("Requesting a trial license contacts the license server")
	void testRequestAnonTrialLicense() {
		LicenseHolder licenseHolderSpy = Mockito.spy(licenseHolder);
		Settings settings = mock(Settings.class);
		LicenseApi.Challenge challenge = mock(LicenseApi.Challenge.class);
		LicenseApi.Solution solution = mock(LicenseApi.Solution.class);
		LicenseApi.TrialLicenseResponse trialLicenseResponse = mock(LicenseApi.TrialLicenseResponse.class);
		doReturn(challenge).when(licenseApi).generateChallenge();
		doReturn("captcha").when(solution).toCaptcha();
		doReturn(solution).when(licenseHolderSpy).solveChallenge(challenge);
		doReturn(trialLicenseResponse).when(licenseApi).generateTrialLicense("captcha");
		doReturn("token").when(trialLicenseResponse).licenseKey();
		doReturn(mock(DecodedJWT.class)).when(validator).validate(Mockito.eq("token"), Mockito.any());

		licenseHolderSpy.requestAnonTrialLicense(settings);

		verify(licenseApi).generateChallenge();
		verify(licenseApi).generateTrialLicense("captcha");
		verify(settings).setHubId(Mockito.any());
		verify(settings).setLicenseKey("token");
		verify(settingsRepo).persistAndFlush(settings);
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
			var decodedJWT = mock(DecodedJWT.class);
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
		private Claim refreshClaim;
		private DecodedJWT licenseJwt;

		@BeforeEach
		void setup() {
			licenseHolderSpy = Mockito.spy(licenseHolder);
			refreshClaim = mock(Claim.class);
			licenseJwt = mock(DecodedJWT.class);

			Mockito.doReturn("http://localhost:3000").when(refreshClaim).asString();
			Mockito.doReturn(refreshClaim).when(licenseJwt).getClaim("refreshUrl");
			Mockito.doReturn("token").when(licenseJwt).getToken();
			Mockito.doReturn(licenseJwt).when(licenseHolderSpy).get();
		}

		@Test
		@DisplayName("If license does not have a refreshUrl, skip refresh")
		void testRefreshLicenseNoRefreshURL() throws LicenseHolder.LicenseRefreshFailedException {
			var missingClaim = mock(Claim.class);
			Mockito.doReturn(true).when(missingClaim).isMissing();
			Mockito.doReturn(missingClaim).when(licenseJwt).getClaim("refreshUrl");

			Assertions.assertThrows(IllegalStateException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}


		@Test
		@DisplayName("If license does not have a valid refreshUrl, throw ISE")
		void testRefreshLicenseBadURL() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn("*:not:an::uri").when(refreshClaim).asString();

			Assertions.assertThrows(IllegalStateException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("If license request throws, do not set license")
		void testRefreshLicenseFailingRequest() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doThrow(new LicenseHolder.LicenseRefreshFailedException("http error 500")).when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Successful refresh request, but failing validation")
		void testRefreshLicenseFailedValidation() throws LicenseHolder.LicenseRefreshFailedException {
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			Mockito.doThrow(JWTVerificationException.class).when(licenseHolderSpy).set("newToken");

			Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, licenseHolderSpy::refreshLicense);

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
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
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
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
			Mockito.doReturn(mock(DecodedJWT.class)).when(validator).validate("newToken", "42");

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
			Mockito.doThrow(JWTVerificationException.class).when(validator).validate("newToken", "42");

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
		void testSucess() throws IOException, InterruptedException, LicenseHolder.LicenseRefreshFailedException {
			URI refreshUrl = URI.create("https://localhost:3000");
			try (var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
				var httpClient = mock(HttpClient.class);
				var httpBuilder = mock(HttpClient.Builder.class);
				when(httpBuilder.build()).thenReturn(httpClient);
				when(httpBuilder.followRedirects(any())).thenReturn(httpBuilder);
				httpClientMock.when(HttpClient::newBuilder).thenReturn(httpBuilder);

				var response = mock(HttpResponse.class);
				when(response.statusCode()).thenReturn(200);
				when(response.body()).thenReturn("newToken");
				when(httpClient.send(argThat(request -> request.uri().equals(refreshUrl)), any())).thenReturn(response);

				var result = licenseHolderSpy.requestLicenseRefresh(refreshUrl, "token");
				Assertions.assertEquals("newToken", result);
			}
		}

		@Test
		void test500Response() throws IOException, InterruptedException {
			URI refreshUrl = URI.create("https://localhost:3000");
			try (var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
				var httpClient = mock(HttpClient.class);
				var httpBuilder = mock(HttpClient.Builder.class);
				when(httpBuilder.build()).thenReturn(httpClient);
				when(httpBuilder.followRedirects(any())).thenReturn(httpBuilder);
				httpClientMock.when(HttpClient::newBuilder).thenReturn(httpBuilder);

				var response = mock(HttpResponse.class);
				when(response.statusCode()).thenReturn(500);
				when(response.body()).thenReturn("newToken");
				when(httpClient.send(argThat(request -> request.uri().equals(refreshUrl)), any())).thenReturn(response);

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolderSpy.requestLicenseRefresh(refreshUrl, "token"));
			}
		}

		@Test
		void testEmtyBody() throws IOException, InterruptedException {
			URI refreshUrl = URI.create("https://localhost:3000");
			try (var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
				var httpClient = mock(HttpClient.class);
				var httpBuilder = mock(HttpClient.Builder.class);
				when(httpBuilder.build()).thenReturn(httpClient);
				when(httpBuilder.followRedirects(any())).thenReturn(httpBuilder);
				httpClientMock.when(HttpClient::newBuilder).thenReturn(httpBuilder);

				var response = mock(HttpResponse.class);
				when(response.statusCode()).thenReturn(200);
				when(response.body()).thenReturn("");
				when(httpClient.send(argThat(request -> request.uri().equals(refreshUrl)), any())).thenReturn(response);

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolderSpy.requestLicenseRefresh(refreshUrl, "token"));
			}
		}
	}

}
