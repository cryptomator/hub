package org.cryptomator.hub.filters;

import io.quarkus.test.junit.QuarkusTest;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

@QuarkusTest
public class ActiveLicenseFilterTest {

	ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	ActiveLicenseFilter filter = new ActiveLicenseFilter();

	@BeforeEach
	public void setup() {
		filter.license = Mockito.mock(LicenseHolder.class);
	}

	@Test
	@DisplayName("abort when providing expired license")
	public void testFilterWithExpiredLicense() {
		Mockito.doReturn(true).when(filter.license).isExpired();

		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Test
	@DisplayName("continue when seats are still available")
	public void testDontFilterWhenLicenseIsNotExpired() {
		Mockito.doReturn(false).when(filter.license).isExpired();

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