package org.cryptomator.hub.license;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

@QuarkusTest
public class SeatsRestrictionFilterTest {

	ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	SeatsRestrictionFilter filter = new SeatsRestrictionFilter();

	@BeforeEach
	public void setup() {
		filter.license = Mockito.mock(LicenseHolder.class);
	}

	@Test
	@DisplayName("abort when providing expired license")
	public void testFilterWithExpiredLicense() {
		Mockito.doReturn(true).when(filter.license).isExpired();
		Mockito.doReturn(Long.MAX_VALUE).when(filter.license).getAvailableSeats();
		PanacheMock.mock(EffectiveVaultAccess.class);
		Mockito.when(EffectiveVaultAccess.countEffectiveVaultUsers()).thenReturn(0L);


		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Test
	@DisplayName("abort when number of available seats is exceeded")
	public void testFilterWhenAllAvailableSeatsUsed() {
		Mockito.doReturn(false).when(filter.license).isExpired();
		Mockito.doReturn(1L).when(filter.license).getAvailableSeats();
		PanacheMock.mock(EffectiveVaultAccess.class);
		Mockito.when(EffectiveVaultAccess.countEffectiveVaultUsers()).thenReturn(1L);

		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Test
	@DisplayName("continue when seats are still available")
	public void testDontFilterWhenSeatsAvailable() {
		Mockito.doReturn(false).when(filter.license).isExpired();
		Mockito.doReturn(Long.MAX_VALUE).when(filter.license).getAvailableSeats();
		PanacheMock.mock(EffectiveVaultAccess.class);
		Mockito.when(EffectiveVaultAccess.countEffectiveVaultUsers()).thenReturn(1L);

		filter.filter(context);

		Mockito.verifyNoInteractions(context);
	}

	private static class StatusCodeMatcher implements ArgumentMatcher<Response> {

		private final int expectedStatusCode;

		private StatusCodeMatcher(int statusCode) {
			this.expectedStatusCode = statusCode;
		}

		public static StatusCodeMatcher hasStatus(Response.Status status) {
			return new StatusCodeMatcher(status.getStatusCode());
		}

		@Override
		public boolean matches(Response response) {
			return response.getStatus() == expectedStatusCode;
		}
	}

}