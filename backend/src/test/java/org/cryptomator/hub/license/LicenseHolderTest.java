package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.cryptomator.hub.entities.Settings;
import org.cryptomator.hub.entities.SettingsRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseHolderTest {

	SettingsRepository settingsRepo = mock(SettingsRepository.class);
	RandomMinuteSleeper randomMinuteSleeper = mock(RandomMinuteSleeper.class);
	LicenseValidator validator = mock(LicenseValidator.class);

	LicenseHolder licenseHolder;

	@BeforeEach
	public void resetTestclass() {
		licenseHolder = new LicenseHolder();
		licenseHolder.licenseValidator = validator;
		licenseHolder.settingsRepo = settingsRepo;
		licenseHolder.randomMinuteSleeper = randomMinuteSleeper;
	}

	@Nested
	@DisplayName("Testing Init Method")
	class TestInit {

		@Test
		@DisplayName("If db token and hubId is set, call validateExisting")
		public void testTokenAndIdPresentInDatabase() {
			//to show check, that db has higher precedence
			licenseHolder.initialId = Optional.of("43");
			licenseHolder.initialLicenseToken = Optional.of("initToken");

			Settings settings = mock(Settings.class);
			when(settings.getLicenseKey()).thenReturn("token");
			when(settings.getHubId()).thenReturn("42");
			when(settingsRepo.get()).thenReturn(settings);

			var licenseHolderSpy = Mockito.spy(licenseHolder);
			licenseHolderSpy.init();
			verify(licenseHolderSpy).validateOrResetExistingLicense(settings);
		}

		@DisplayName("If dbToken or dbHubId is null, use set init config values")
		@ParameterizedTest
		@MethodSource("provideInitValuesCases")
		public void testInitValues(String dbToken, String dbHubId) {
			Settings settings = mock(Settings.class);
			when(settings.getLicenseKey()).thenReturn(dbToken);
			when(settings.getHubId()).thenReturn(dbHubId);
			when(settingsRepo.get()).thenReturn(settings);

			licenseHolder.initialLicenseToken = Optional.of("token");
			licenseHolder.initialId = Optional.of("43");

			var licenseHolderSpy = Mockito.spy(licenseHolder);
			licenseHolderSpy.init();
			verify(licenseHolderSpy).validateAndApplyInitLicense(settings, "token", "43");
		}

		public static Stream<Arguments> provideInitValuesCases() {
			return Stream.of(
					Arguments.of("dbToken", null),
					Arguments.of(null, null),
					Arguments.of(null, "42")
			);
		}

		@DisplayName("Do nothing, if db and init have a null value")
		@ParameterizedTest
		@MethodSource("provideDoNothingCases")
		public void testDoNothingCases(String dbToken, String dbHubId, String initToken, String initId) {
			Settings settings = mock(Settings.class);
			when(settings.getLicenseKey()).thenReturn(dbToken);
			when(settings.getHubId()).thenReturn(dbHubId);
			when(settingsRepo.get()).thenReturn(settings);

			licenseHolder.initialLicenseToken = Optional.ofNullable(initToken);
			licenseHolder.initialId = Optional.ofNullable(initId);

			var licenseHolderSpy = Mockito.spy(licenseHolder);
			licenseHolderSpy.init();
			verify(licenseHolderSpy, never()).validateOrResetExistingLicense(settings);
			verify(licenseHolderSpy, never()).validateAndApplyInitLicense(Mockito.eq(settings), any(), any());
		}

		public static Stream<Arguments> provideDoNothingCases() {
			return Stream.of(
					Arguments.of("dbToken", null, null, "43"),
					Arguments.of(null, "42", null, "43"),
					Arguments.of(null, "42", "initToken", null),
					Arguments.of("dbToken", null, "initToken", null)
			);
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

		licenseHolder.validateOrResetExistingLicense(settings);
		verify(settings, never()).setHubId(any());
		verify(settings, never()).setLicenseKey(any());
	}

	@Test
	@DisplayName("Invalid db token is set to null and persisted")
	public void testValidateExistingFailure() {
		Settings settings = mock(Settings.class);
		when(settings.getLicenseKey()).thenReturn("token");
		when(settings.getHubId()).thenReturn("42");
		when(settingsRepo.get()).thenReturn(settings);

		when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);

		licenseHolder.validateOrResetExistingLicense(settings);
		verify(settings, never()).setHubId(any());
		verify(settings).setLicenseKey(Mockito.isNull());
		verify(settingsRepo).persistAndFlush(settings);
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

		licenseHolder.validateAndApplyInitLicense(settings, "token", "42");
		verify(settings, never()).setHubId(any());
		verify(settings, never()).setLicenseKey(any());
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
			when(validator.validate("token", "42")).thenAnswer(invocationOnMock -> {
				throw new JWTVerificationException("");
			});
			Settings settings = mock(Settings.class);
			when(settings.getHubId()).thenReturn("42");
			when(settingsRepo.get()).thenReturn(settings);

			Assertions.assertThrows(JWTVerificationException.class, () -> licenseHolder.set("token"));

			verify(validator).validate("token", "42");
			verify(settingsRepo, never()).persist((Settings) any());
			Assertions.assertNull(licenseHolder.get()); //TODO: not very unit test like
		}
	}

	@Nested
	@DisplayName("Testing refreshLicense()")
	class RefreshLicense {

		@Test
		@DisplayName("If license does not have a refreshUrl, skip refresh")
		public void testRefreshLicenseNoRefreshURL() throws InterruptedException, IOException {
			var licenseHolderSpy = Mockito.spy(licenseHolder);

			var licenseJwt = mock(DecodedJWT.class);
			when(licenseJwt.getClaim("refreshUrl")).thenReturn(null);
			when(licenseHolderSpy.get()).thenReturn(licenseJwt);

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy, never()).requestLicenseRefresh(any(), any());
			verify(licenseHolderSpy, never()).set(any());
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}


		@Test
		@DisplayName("If license does not have a valid refreshUrl, skip refresh")
		public void testRefreshLicenseBadURL() throws InterruptedException, IOException {
			var licenseHolderSpy = Mockito.spy(licenseHolder);

			var refreshClaim = mock(Claim.class);
			when(refreshClaim.asString()).thenReturn("*:not:an::uri");
			var licenseJwt = mock(DecodedJWT.class);
			when(licenseJwt.getClaim("refreshUrl")).thenReturn(refreshClaim);
			when(licenseHolderSpy.get()).thenReturn(licenseJwt);

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
			var licenseHolderSpy = Mockito.spy(licenseHolder);

			var refreshClaim = mock(Claim.class);
			when(refreshClaim.asString()).thenReturn("http://localhost:3000");
			var licenseJwt = mock(DecodedJWT.class);
			when(licenseJwt.getClaim("refreshUrl")).thenReturn(refreshClaim);
			when(licenseJwt.getToken()).thenReturn("token");
			when(licenseHolderSpy.get()).thenReturn(licenseJwt);
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
			var licenseHolderSpy = Mockito.spy(licenseHolder);

			var refreshClaim = mock(Claim.class);
			when(refreshClaim.asString()).thenReturn("http://localhost:3000");
			var licenseJwt = mock(DecodedJWT.class);
			when(licenseJwt.getClaim("refreshUrl")).thenReturn(refreshClaim);
			when(licenseJwt.getToken()).thenReturn("token");
			when(licenseHolderSpy.get()).thenReturn(licenseJwt);
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			Mockito.doThrow(JWTVerificationException.class).when(licenseHolderSpy).set("newToken");

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			verify(licenseHolderSpy).set("newToken");
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

		@Test
		@DisplayName("Successful refresh request, but failing validation")
		public void testRefreshLicenseSuccess() throws InterruptedException, IOException {
			var licenseHolderSpy = Mockito.spy(licenseHolder);

			var refreshClaim = mock(Claim.class);
			when(refreshClaim.asString()).thenReturn("http://localhost:3000");
			var licenseJwt = mock(DecodedJWT.class);
			when(licenseJwt.getClaim("refreshUrl")).thenReturn(refreshClaim);
			when(licenseJwt.getToken()).thenReturn("token");
			when(licenseHolderSpy.get()).thenReturn(licenseJwt);
			Mockito.doReturn("newToken").when(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			Mockito.doThrow(JWTVerificationException.class).when(licenseHolderSpy).set("newToken");

			licenseHolderSpy.refreshLicense();

			verify(licenseHolderSpy).requestLicenseRefresh(any(), eq("token"));
			verify(licenseHolderSpy).set("newToken");
			verify(settingsRepo, never()).get();
			verify(settingsRepo, never()).persistAndFlush(any());
		}

	}

	@Nested
	@DisplayName("Testing requestLicenseRefresh()")
	class RequestLicenseRefresh {

		@Test
		public void testSucess() throws IOException, InterruptedException {
			URI refreshUrl = URI.create("https://localhost:3000");
			try( var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
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
			try( var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
				var httpClient = mock(HttpClient.class);
				var httpBuilder = mock(HttpClient.Builder.class);
				when(httpBuilder.build()).thenReturn(httpClient);
				when(httpBuilder.followRedirects(any())).thenReturn(httpBuilder);
				httpClientMock.when(HttpClient::newBuilder).thenReturn(httpBuilder);

				var response = mock(HttpResponse.class);
				when(response.statusCode()).thenReturn(500);
				when(response.body()).thenReturn("newToken");
				when(httpClient.send(argThat(request -> request.uri().equals(refreshUrl)), any())).thenReturn(response);

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class,() -> licenseHolder.requestLicenseRefresh(refreshUrl, "token"));
			}
		}

		@Test
		public void testEmtyBody() throws IOException, InterruptedException {
			URI refreshUrl = URI.create("https://localhost:3000");
			try( var httpClientMock = Mockito.mockStatic(HttpClient.class)) {
				var httpClient = mock(HttpClient.class);
				var httpBuilder = mock(HttpClient.Builder.class);
				when(httpBuilder.build()).thenReturn(httpClient);
				when(httpBuilder.followRedirects(any())).thenReturn(httpBuilder);
				httpClientMock.when(HttpClient::newBuilder).thenReturn(httpBuilder);

				var response = mock(HttpResponse.class);
				when(response.statusCode()).thenReturn(200);
				when(response.body()).thenReturn("");
				when(httpClient.send(argThat(request -> request.uri().equals(refreshUrl)), any())).thenReturn(response);

				Assertions.assertThrows(LicenseHolder.LicenseRefreshFailedException.class,() -> licenseHolder.requestLicenseRefresh(refreshUrl, "token"));
			}
		}
	}

}
