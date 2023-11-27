package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.arc.Arc;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Settings;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

@QuarkusTest
public class LicenseHolderTest {

	@Inject
	LicenseHolder holder;

	@Nested
	@DisplayName("Testing PostConstruct Method")
	class TestPostConstruct {

		@InjectMock
		Session session;

		@InjectMock
		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			settingsClass = Mockito.mockStatic(Settings.class);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
			Arc.container().instance(LicenseHolder.class).destroy();
		}

		@Test
		@DisplayName("If database token is valid, set it in license holder")
		public void testValidDBTokenSet() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(decodedJWT);
			Settings settingsMock = new Settings();
			settingsMock.licenseKey = "token";
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(2)).validate("token", "42");
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("If database token is invalid, do net set it in license holder and nullify db entry")
		public void testDBTokenOnFailedValidationNotSet() {
			Mockito.when(validator.validate(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocationOnMock -> {
				throw new JWTVerificationException("");
			});
			Settings settingsMock = new Settings();
			settingsMock.licenseKey = "token";
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.eq(settingsMock));
			Assertions.assertNull(holder.get());
		}

		@Test
		@DisplayName("If database token is null, do net set it in license holder")
		public void testNullDBTokenNotSet() {
			Settings settingsEntity = Mockito.mock(Settings.class);
			settingsClass.when(Settings::get).thenReturn(settingsEntity);

			holder.init();

			Mockito.verify(validator, Mockito.never()).validate(Mockito.anyString(), Mockito.anyString());
			Assertions.assertNull(holder.get());
		}
	}

	// -- set --

	@Nested
	@DisplayName("Testing  set() method of LicenseHolder")
	class TestSetter {

		@InjectMock
		Session session;

		@InjectMock
		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			settingsClass = Mockito.mockStatic(Settings.class);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
			Arc.container().instance(LicenseHolder.class).destroy();
		}

		@Test
		@DisplayName("Setting a valid token validates and persists it to db")
		public void testSetValidToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(decodedJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.set("token");

			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.eq(settingsMock));
			Assertions.assertEquals("token", settingsMock.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("Setting an invalid token fails with exception")
		public void testSetInvalidToken() {
			Mockito.when(validator.validate("token", "42")).thenAnswer(invocationOnMock -> {
				throw new JWTVerificationException("");
			});
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			Assertions.assertThrows(JWTVerificationException.class, () -> holder.set("token"));

			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertNull(holder.get());
		}
	}

	@Nested
	@DisplayName("Testing refreshLicense() method of LicenseHolder")
	class TestRefreshLicense {

		private final String refreshURL = "https://foo.bar.baz/";

		private final HttpRequest refreshRequst = HttpRequest.newBuilder() //
				.uri(URI.create(refreshURL)) //
				.headers("Content-Type", "application/x-www-form-urlencoded") //
				.POST(HttpRequest.BodyPublishers.ofString("token=token"))  //
				.build();

		@InjectMock
		Session session;

		@InjectMock
		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			settingsClass = Mockito.mockStatic(Settings.class);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
			Arc.container().instance(LicenseHolder.class).destroy();
		}

		@Test
		@DisplayName("Refreshing a valid token validates and persists it to db")
		public void testRefreshingExistingValidTokenInculdingRefreshURL() throws IOException, InterruptedException {
			var existingJWT = Mockito.mock(DecodedJWT.class);
			var receivedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(existingJWT.getToken()).thenReturn("token");
			Mockito.when(validator.validate("token", "42")).thenReturn(receivedJWT);
			Mockito.when(validator.validate("oldToken", "42")).thenReturn(existingJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsMock.licenseKey = "oldToken";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			var httpClient = Mockito.mock(HttpClient.class);
			var response = Mockito.mock(HttpResponse.class);
			Mockito.doAnswer(invocation -> {
				HttpRequest httpRequest = invocation.getArgument(0);
				Assertions.assertEquals(refreshRequst, httpRequest);
				return response;
			}).when(httpClient).send(Mockito.any(), Mockito.eq(HttpResponse.BodyHandlers.ofString()));
			Mockito.when(response.body()).thenReturn("token");
			Mockito.when(response.statusCode()).thenReturn(200);

			holder.refreshLicense(refreshURL, existingJWT.getToken(), httpClient);

			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.eq(settingsMock));
			Assertions.assertEquals(receivedJWT, holder.get());
		}

		@ParameterizedTest(name = "Refreshing a valid token but receiving \"{0}\" with status code does \"{1}\" not persists it to db")
		@CsvSource(value = {"invalidToken,200", "'',200", "validToken,500"})
		public void testInvalidTokenReceivedLeadsToNoOp(String receivedToken, int receivedCode) throws IOException, InterruptedException {
			var existingJWT = Mockito.mock(DecodedJWT.class);
			var receivedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(existingJWT.getToken()).thenReturn("token");
			Mockito.when(validator.validate("token", "42")).thenReturn(existingJWT);
			if (receivedToken.equals("validToken")) {
				Mockito.when(validator.validate(receivedToken, "42")).thenReturn(receivedJWT);
			} else {
				Mockito.when(validator.validate(receivedToken, "42")).thenAnswer(invocationOnMock -> {
					throw new JWTVerificationException("");
				});
			}
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsMock.licenseKey = "token";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			var httpClient = Mockito.mock(HttpClient.class);
			var response = Mockito.mock(HttpResponse.class);
			Mockito.doAnswer(invocation -> {
				HttpRequest httpRequest = invocation.getArgument(0);
				Assertions.assertEquals(refreshRequst, httpRequest);
				return response;
			}).when(httpClient).send(Mockito.any(), Mockito.eq(HttpResponse.BodyHandlers.ofString()));
			Mockito.when(response.body()).thenReturn(receivedToken);
			Mockito.when(response.statusCode()).thenReturn(receivedCode);

			holder.refreshLicense(refreshURL, existingJWT.getToken(), httpClient);

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertEquals(existingJWT, holder.get());
		}

		@Test
		@DisplayName("Refreshing a valid token but IOException thrown does not persists it to db")
		public void testCommunicationProblemLeadsToNoOp() throws IOException, InterruptedException {
			var existingJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(existingJWT.getToken()).thenReturn("token");
			Mockito.when(validator.validate("token", "42")).thenReturn(existingJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsMock.licenseKey = "token";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			var httpClient = Mockito.mock(HttpClient.class);
			Mockito.doAnswer(invocation -> {
				HttpRequest httpRequest = invocation.getArgument(0);
				Assertions.assertEquals(refreshRequst, httpRequest);
				throw new IOException("Problem during communication");
			}).when(httpClient).send(Mockito.any(), Mockito.eq(HttpResponse.BodyHandlers.ofString()));

			holder.refreshLicense(refreshURL, existingJWT.getToken(), httpClient);

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(1)).validate("token", "42");
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertEquals(existingJWT, holder.get());
		}

		@Test
		@DisplayName("Refreshing a valid token without refresh URL does not execute refreshLicense")
		public void testNoOpExistingValidTokenExculdingRefreshURL() throws InterruptedException {
			var existingJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(existingJWT);
			Mockito.when(validator.refreshUrl(existingJWT.getToken())).thenReturn(Optional.empty());
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsMock.licenseKey = "token";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.refreshLicenseScheduler();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.any(), Mockito.any());
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertEquals(existingJWT, holder.get());
		}
	}

	@Nested
	@TestProfile(LicenseHolderInitPropsTest.ValidInitPropsInstanceTestProfile.class)
	@DisplayName("Testing LicenseHolder methods using InitProps")
	class LicenseHolderInitPropsTest {

		@InjectMock
		Session session;

		@InjectMock
		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		public static class ValidInitPropsInstanceTestProfile implements QuarkusTestProfile {
			@Override
			public Map<String, String> getConfigOverrides() {
				return Map.of("hub.initial-id", "42", "hub.initial-license", "token");
			}
		}

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			settingsClass = Mockito.mockStatic(Settings.class);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
			Arc.container().instance(LicenseHolder.class).destroy();
		}

		@Test
		@DisplayName("If init token is valid, set it in license holder")
		public void testValidInitTokenSet() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(decodedJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(2)).validate("token", "42");
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("If init token is invalid and no token is set in db, do not modify db")
		public void testInitTokenOnFailedValidationNotSet() {
			Mockito.when(validator.validate("token", "42")).thenAnswer(invocationOnMock -> {
				throw new JWTVerificationException("");
			});
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(2)).validate("token", "42");
			Mockito.verify(session, Mockito.times(2)).persist(Mockito.eq(settingsMock));
			Assertions.assertNull(holder.get());
		}

		@Test
		@DisplayName("If token is set in DB, ignore valid init token")
		public void testValidDBTokenIgnoresValidInitToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token3000", "3000")).thenReturn(decodedJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "3000";
			settingsMock.licenseKey = "token3000";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(2)).validate("token3000", "3000");
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("If token is set in DB, ignore invalid init token")
		public void testValidDBTokenIgnoresInvalidInitToken() {
			Mockito.when(validator.validate("token", "42")).thenAnswer(invocationOnMock -> {
				throw new JWTVerificationException("");
			});

			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token3000", "3000")).thenReturn(decodedJWT);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "3000";
			settingsMock.licenseKey = "token3000";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			// init implicitly called due to @PostConstruct which increases the times to verify by 1
			// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
			Mockito.verify(validator, Mockito.times(2)).validate("token3000", "3000");
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("Setting a valid token validates and overwrites the init token")
		public void testSetValidToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token3000", "42")).thenReturn(decodedJWT);

			Settings initSettingsMock = new Settings();
			initSettingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(initSettingsMock);

			Settings persistingSettingsMock = new Settings();
			persistingSettingsMock.hubId = "42";
			persistingSettingsMock.licenseKey = "token3000";

			holder.set("token3000");

			Mockito.verify(validator, Mockito.times(1)).validate("token3000", "42");
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.eq(persistingSettingsMock));
			Assertions.assertEquals(decodedJWT, holder.get());
		}

	}

}

