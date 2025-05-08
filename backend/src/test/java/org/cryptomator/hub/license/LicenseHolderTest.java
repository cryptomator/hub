package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.test.InjectMock;
import org.cryptomator.hub.entities.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LicenseHolderTest {

	Settings.Repository settingsRepo = mock(Settings.Repository.class);
	RandomMinuteSleeper randomMinuteSleeper = mock(RandomMinuteSleeper.class);
	LicenseValidator validator = mock(LicenseValidator.class);
	LicenseApi licenseApi = mock(LicenseApi.class);

	LicenseHolder licenseHolder;

	@BeforeEach
	public void resetTestclass() {
		licenseHolder = new LicenseHolder();
		licenseHolder.licenseValidator = validator;
		licenseHolder.settingsRepo = settingsRepo;
		licenseHolder.randomMinuteSleeper = randomMinuteSleeper;
		licenseHolder.licenseApi = licenseApi;
	}

	@Nested
	@DisplayName("Testing ensureLicenseExists()")
	class TestEnsureLicenseExists {

		private Settings settings;
		private LicenseHolder licenseHolderSpy;

		@BeforeEach
		public void setup() {
			settings = mock(Settings.class);
			licenseHolderSpy = Mockito.spy(licenseHolder);
			Mockito.doReturn(settings).when(settingsRepo).get();
			Mockito.doNothing().when(licenseHolderSpy).validateExistingLicense(any());
			Mockito.doNothing().when(licenseHolderSpy).validateAndApplyInitLicense(any(), any(), any());
			Mockito.doNothing().when(licenseHolderSpy).requestAnonTrialLicense(settings);
		}

		@Test
		@DisplayName("call validateExistingLicense(), if DB contains existing token")
		public void testValidateExistingLicense() {
			//to show check, that db has higher precedence
			licenseHolderSpy.initialId = Optional.of("43");
			licenseHolderSpy.initialLicenseToken = Optional.of("initToken");
			when(settings.getLicenseKey()).thenReturn("token");
			when(settings.getHubId()).thenReturn("42");

			licenseHolderSpy.ensureLicenseExists();

			verify(licenseHolderSpy).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(any(), any(), any());
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(settings);
		}

		@DisplayName("call validateAndApplyInitLicense(), if DB doesn't contain token but init config does")
		@ParameterizedTest
		@CsvSource(value = {
				"dbToken, null",
				"null, null",
				"null, 42"
		}, nullValues = {"null"})
		public void testApplyInitLicense(String dbToken, String dbHubId) {
			licenseHolderSpy.initialLicenseToken = Optional.of("token");
			licenseHolderSpy.initialId = Optional.of("43");
			when(settings.getLicenseKey()).thenReturn(dbToken);
			when(settings.getHubId()).thenReturn(dbHubId);

			licenseHolderSpy.ensureLicenseExists();

			verify(licenseHolderSpy, never()).validateExistingLicense(any());
			verify(licenseHolderSpy).validateAndApplyInitLicense(settings, "token", "43");
			verify(licenseHolderSpy, never()).requestAnonTrialLicense(settings);
		}

		@DisplayName("call requestAnonTrialLicense(), if neither DB nor init config contains token")
		@ParameterizedTest
		@CsvSource(value = {
				"dbToken, null, null, 43",
				"null, 42, null, 43",
				"dbToken, null, initToken, null"
		}, nullValues = {"null"})
		public void testRequestTrialLicense(String dbToken, String dbHubId, String initToken, String initId) {
			licenseHolderSpy.initialLicenseToken = Optional.ofNullable(initToken);
			licenseHolderSpy.initialId = Optional.ofNullable(initId);
			when(settings.getLicenseKey()).thenReturn(dbToken);
			when(settings.getHubId()).thenReturn(dbHubId);

			licenseHolderSpy.ensureLicenseExists();

			verify(licenseHolderSpy, never()).validateExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(Mockito.eq(settings), any(), any());
			verify(licenseHolderSpy).requestAnonTrialLicense(settings);
		}
	}

	@Test
	@DisplayName("Valid db token does not change settings")
	public void testValidateExistingSuccess() {
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
	public void testValidateExistingFailure() {
		Settings settings = mock(Settings.class);
		when(settings.getLicenseKey()).thenReturn("token");
		when(settings.getHubId()).thenReturn("42");
		when(settingsRepo.get()).thenReturn(settings);
		when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);

		Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.validateExistingLicense(settings));
	}

	@Test
	@DisplayName("Valid init token is persisted with hubID to db")
	public void testApplyInitSuccess() {
		Settings settings = mock(Settings.class);
		when(validator.validate("token", "42")).thenReturn(Mockito.mock(DecodedJWT.class));

		licenseHolder.validateAndApplyInitLicense(settings, "token", "42");

		verify(settings).setHubId("42");
		verify(settings).setLicenseKey("token");
		verify(settingsRepo).persistAndFlush(settings);
	}

	@Test
	@DisplayName("Invalid init token does not change settings")
	public void testApplyInitFailure() {
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
	public void testRequestAnonTrialLicense() {
		LicenseHolder licenseHolderSpy = Mockito.spy(licenseHolder);
		Settings settings = mock(Settings.class);
		LicenseApi.Challenge challenge = mock(LicenseApi.Challenge.class);
		doReturn(challenge).when(licenseApi).generateTrialChallenge(Mockito.any());
		doReturn(1337).when(licenseHolderSpy).solveChallenge(challenge);
		doReturn("token").when(licenseApi).verifyTrialChallenge(Mockito.any(), Mockito.eq(1337));
		doReturn(mock(DecodedJWT.class)).when(validator).validate(Mockito.eq("token"), Mockito.any());

		licenseHolderSpy.requestAnonTrialLicense(settings);

		verify(licenseApi).generateTrialChallenge(Mockito.any());
		verify(licenseApi).verifyTrialChallenge(Mockito.any(), Mockito.eq(1337));
		verify(settings).setHubId(Mockito.any());
		verify(settings).setLicenseKey("token");
		verify(settingsRepo).persistAndFlush(settings);
	}

	@Nested
	@DisplayName("Testing  set() method")
	class TestSetter {

		@BeforeEach
		public void setup() throws InterruptedException {
			Mockito.doNothing().when(randomMinuteSleeper).sleep();
		}

		@Test
		@DisplayName("Setting a valid token validates and persists it to db")
		public void testSetValidToken() {
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
		public void testSetInvalidToken() {
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
		public void setup() {
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
		public void testRefreshLicenseNoRefreshURL() throws InterruptedException, IOException {
			Mockito.doReturn(null).when(licenseJwt).getClaim("refreshUrl");

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}


		@Test
		@DisplayName("If license does not have a valid refreshUrl, skip refresh")
		public void testRefreshLicenseBadURL() throws InterruptedException, IOException {
			Mockito.doReturn("*:not:an::uri").when(refreshClaim).asString();

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@DisplayName("If license request throws, do not set license")
		@ParameterizedTest
		@MethodSource("provideRefreshLicenseFailingRequestCases")
		public void testRefreshLicenseFailingRequest(Throwable t) throws InterruptedException, IOException {
			Mockito.doThrow(t).when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		static Stream<Throwable> provideRefreshLicenseFailingRequestCases() {
			return Stream.of(new IOException(), new LicenseHolder.LicenseRefreshFailedException(500, "Server Error"));
		}

		@Test
		@DisplayName("Successful refresh request, but failing validation")
		public void testRefreshLicenseFailedValidation() throws InterruptedException, IOException {
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			Mockito.doThrow(JWTVerificationException.class).when(licenseHolderSpy).set("newToken");

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			verify(licenseHolderSpy).set("newToken");
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Successful refresh")
		public void testRefreshLicenseSuccess() throws InterruptedException, IOException {
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
	@DisplayName("Testing requestLicenseRefresh()")
	class RequestLicenseRefresh {

		@Test
		public void testSucess() throws IOException, InterruptedException {
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

				var result = licenseHolder.requestLicenseRefresh(refreshUrl, "token");
				Assertions.assertEquals("newToken", result);
			}
		}

		@Test
		public void test500Response() throws IOException, InterruptedException {
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

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolder.requestLicenseRefresh(refreshUrl, "token"));
			}
		}

		@Test
		public void testEmtyBody() throws IOException, InterruptedException {
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

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class, () -> licenseHolder.requestLicenseRefresh(refreshUrl, "token"));
			}
		}
	}

}
