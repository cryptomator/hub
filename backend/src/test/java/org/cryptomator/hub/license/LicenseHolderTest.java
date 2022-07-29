package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
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


@QuarkusTest
public class LicenseHolderTest {

	LicenseHolder holder;

	@Nested
	@DisplayName("Testing PostConstruct Method")
	class TestPostConstruct {

		@InjectMock
		Session session;

		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			validator = Mockito.mock(LicenseValidator.class);
			settingsClass = Mockito.mockStatic(Settings.class);
			holder = new LicenseHolder(validator);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
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

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("If database token is invalid, do net set it in license holder and nullify db entry")
		public void testDBTokenOnFailedValidationNotSet() {
			Mockito.when(validator.validate(Mockito.anyString(), Mockito.anyString())).thenThrow(JWTVerificationException.class);
			Settings settingsMock = new Settings();
			settingsMock.licenseKey = "token";
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			holder.init();

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.any());
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

		LicenseValidator validator;

		MockedStatic<Settings> settingsClass;

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);

			validator = Mockito.mock(LicenseValidator.class);
			settingsClass = Mockito.mockStatic(Settings.class);
			holder = new LicenseHolder(validator);
		}

		@AfterEach
		public void teardown() {
			settingsClass.close();
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

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.any());
			Assertions.assertEquals("token", settingsMock.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("Setting an invalid token fails with exception")
		public void testSetInvalidToken() {
			Mockito.when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);
			Settings settingsMock = new Settings();
			settingsMock.hubId = "42";
			settingsClass.when(Settings::get).thenReturn(settingsMock);

			Assertions.assertThrows(JWTVerificationException.class, () -> holder.set("token"));

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertNull(holder.get());
		}
	}


}

