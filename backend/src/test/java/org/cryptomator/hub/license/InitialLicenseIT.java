package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.arc.Arc;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

@QuarkusTest
@TestProfile(InitialLicenseIT.ValidInitPropsInstanceTestProfile.class)
public class InitialLicenseIT {

	@Inject
	LicenseHolder holder;

	@InjectMock
	LicenseValidator validator;

	Settings settings = Mockito.spy(new Settings());

	@InjectMock
	RandomMinuteSleeper randomMinuteSleeper;

	@BeforeEach
	public void setup( ) throws InterruptedException {
		Mockito.doNothing().when(randomMinuteSleeper).sleep();
		PanacheMock.mock(Settings.class); // see https://quarkus.io/guides/hibernate-orm-panache#mocking
		Mockito.when(Settings.get()).thenReturn(settings);
		Mockito.doNothing().when(settings).persistAndFlush();

		// recreate bean to start with a fresh instance:
		// init() implicitly called due to @PostConstruct which messes with our invocation count
		// See https://github.com/cryptomator/hub/pull/229#discussion_r1374694626 for further information
		try (var instance = Arc.container().instance(LicenseHolder.class)) {
			if (instance.isAvailable()) {
				instance.destroy();
				Assertions.assertDoesNotThrow(holder::isSet); // recreate
			}
		}
		Mockito.clearInvocations(validator);
		Mockito.clearInvocations(settings);
	}

	@Nested
	@DisplayName("If no token is stored in DB")
	public class WithoutTokenInDatabase {

		@BeforeEach
		public void setup() {
			settings.hubId = "42";
			settings.licenseKey = null;
		}

		@Test
		@DisplayName("persist hub.initial-license if it is valid")
		public void testValidInitTokenSet() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("initialToken", "42")).thenReturn(decodedJWT);

			holder.init();

			Mockito.verify(validator).validate("initialToken", "42");
			Mockito.verify(settings).persistAndFlush();
			Assertions.assertEquals("initialToken", settings.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("no-op if hub.initial-license is invalid")
		public void testInitTokenOnFailedValidationNotSet() {
			Mockito.when(validator.validate("initialToken", "42")).thenThrow(new JWTVerificationException(""));

			holder.init();

			Mockito.verify(validator).validate("initialToken", "42");
			Mockito.verify(settings, Mockito.never()).persistAndFlush();
			Assertions.assertNull(settings.licenseKey);
			Assertions.assertNull(holder.get());
		}

	}

	@Nested
	@DisplayName("If an existing token is stored in DB")
	public class WithTokenInDatabase {

		@BeforeEach
		public void setup() {
			settings.hubId = "42";
			settings.licenseKey = "oldToken";
		}

		@Test
		@DisplayName("valid hub.initial-license is ignored")
		public void testValidDBTokenIgnoresValidInitToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("oldToken", "42")).thenReturn(decodedJWT);
			settings.hubId = "42";
			settings.licenseKey = "oldToken";

			holder.init();

			Mockito.verify(validator, Mockito.never()).validate("initialToken", "42");
			Mockito.verify(validator).validate("oldToken", "42");
			PanacheMock.verify(Settings.class, Mockito.never()).persistAndFlush();
			Assertions.assertEquals("oldToken", settings.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("invalid hub.initial-license is ignored")
		public void testValidDBTokenIgnoresInvalidInitToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("oldToken", "42")).thenReturn(decodedJWT);
			settings.hubId = "42";
			settings.licenseKey = "oldToken";

			holder.init();

			Mockito.verify(validator, Mockito.never()).validate("initialToken", "42");
			Mockito.verify(validator).validate("oldToken", "42");
			Mockito.verify(settings, Mockito.never()).persistAndFlush();
			Assertions.assertEquals("oldToken", settings.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("explicitly setting a new token overwrites the existing one")
		public void testSetValidToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("newToken", "42")).thenReturn(decodedJWT);
			settings.hubId = "42";
			settings.licenseKey = "oldToken";

			holder.set("newToken");

			Mockito.verify(validator).validate("newToken", "42");
			Mockito.verify(settings).persistAndFlush();
			Assertions.assertEquals("newToken", settings.licenseKey);
			Assertions.assertEquals(decodedJWT, holder.get());
		}


	}


	public static class ValidInitPropsInstanceTestProfile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of("hub.initial-id", "42", "hub.initial-license", "initialToken");
		}

		@Override
		public boolean disableGlobalTestResources() {
			return true;
		}
	}
}
