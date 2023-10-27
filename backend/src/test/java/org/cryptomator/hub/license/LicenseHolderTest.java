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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

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

