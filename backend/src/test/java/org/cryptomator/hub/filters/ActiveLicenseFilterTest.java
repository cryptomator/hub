package org.cryptomator.hub.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class ActiveLicenseFilterTest {

	ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	LicenseHolder license = Mockito.mock(LicenseHolder.class);
	ActiveLicenseFilter filter = new ActiveLicenseFilter(license);

	@Test
	@DisplayName("abort when providing expired license")
	void testFilterWithExpiredLicense() {
		Mockito.doReturn(true).when(license).isExpired();

		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Test
	@DisplayName("continue when seats are still available")
	void testDontFilterWhenLicenseIsNotExpired() {
		Mockito.doReturn(false).when(license).isExpired();

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