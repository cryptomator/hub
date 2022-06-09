package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.cryptomator.hub.entities.Billing;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


@QuarkusTest
public class LicenseHolderTest {

	LicenseHolder holder;

	@Nested
	@DisplayName("Testing PostConstruct Method")
	class TestPostConstruct {

		LicenseValidator validator;

		@BeforeEach
		public void setup() {
			this.validator = Mockito.mock(LicenseValidator.class);
			holder = new LicenseHolder(validator);
		}

		@Test
		@DisplayName("If database token is valid, set it in license holder")
		public void testValidDBTokenSet() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(decodedJWT);
			Billing billingMock = new Billing();
			billingMock.token = "token";
			billingMock.hubId = "42";
			PanacheQuery<Billing> query = Mockito.mock(PanacheQuery.class);
			Mockito.when(query.firstResult()).thenReturn(billingMock);
			PanacheMock.mock(Billing.class);
			Mockito.when(Billing.<Billing>findAll()).thenReturn(query);

			holder.init();

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("If database token is invalid, do net set it in license holder")
		public void testDBTokenOnFailedValidationNotSet() {
			Mockito.when(validator.validate(Mockito.anyString(), Mockito.anyString())).thenThrow(JWTVerificationException.class);
			Billing billingMock = new Billing();
			billingMock.token = "token";
			billingMock.hubId = "42";
			PanacheQuery<Billing> query = Mockito.mock(PanacheQuery.class);
			Mockito.when(query.firstResult()).thenReturn(billingMock);
			PanacheMock.mock(Billing.class);
			Mockito.when(Billing.<Billing>findAll()).thenReturn(query);

			holder.init();

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Assertions.assertNull(holder.get());
		}

		@Test
		@DisplayName("If database token is null, do net set it in license holder")
		public void testNullDBTokenNotSet() {
			Billing billingEntity = Mockito.mock(Billing.class);
			PanacheQuery<Billing> query = Mockito.mock(PanacheQuery.class);
			Mockito.when(query.firstResult()).thenReturn(billingEntity);
			PanacheMock.mock(Billing.class);
			Mockito.when(Billing.<Billing>findAll()).thenReturn(query);
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

		@BeforeEach
		public void setup() {
			Query mockQuery = Mockito.mock(Query.class);
			Mockito.doNothing().when(session).persist(Mockito.any());
			Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(mockQuery);
			Mockito.when(mockQuery.getSingleResult()).thenReturn(0l);


			this.validator = Mockito.mock(LicenseValidator.class);
			holder = new LicenseHolder(validator);
		}

		@Test
		@DisplayName("Setting a valid token validates and persists it to db")
		public void testSetValidToken() {
			var decodedJWT = Mockito.mock(DecodedJWT.class);
			Mockito.when(validator.validate("token", "42")).thenReturn(decodedJWT);
			Billing billingMock = new Billing();
			billingMock.hubId = "42";
			PanacheQuery<Billing> query = Mockito.mock(PanacheQuery.class);
			Mockito.when(query.firstResult()).thenReturn(billingMock);
			PanacheMock.mock(Billing.class);
			Mockito.when(Billing.<Billing>findAll()).thenReturn(query);

			holder.set("token");

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Mockito.verify(session, Mockito.times(1)).persist(Mockito.any());
			Assertions.assertEquals("token", billingMock.token);
			Assertions.assertEquals(decodedJWT, holder.get());
		}

		@Test
		@DisplayName("Setting an invalid token fails with exception")
		public void testSetInvalidToken() {
			Mockito.when(validator.validate("token", "42")).thenThrow(JWTVerificationException.class);
			Billing billingMock = new Billing();
			billingMock.hubId = "42";
			PanacheQuery<Billing> query = Mockito.mock(PanacheQuery.class);
			Mockito.when(query.firstResult()).thenReturn(billingMock);
			PanacheMock.mock(Billing.class);
			Mockito.when(Billing.<Billing>findAll()).thenReturn(query);

			Assertions.assertThrows(JWTVerificationException.class, () -> holder.set("token"));

			Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq("token"), Mockito.eq("42"));
			Mockito.verify(session, Mockito.never()).persist(Mockito.any());
			Assertions.assertNull(holder.get());
		}
	}


}

