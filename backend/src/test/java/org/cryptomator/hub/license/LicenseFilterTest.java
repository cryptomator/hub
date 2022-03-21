package org.cryptomator.hub.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.util.EnumSet;

public class LicenseFilterTest {

	ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	LicenseFilter filter = new LicenseFilter();

	@BeforeEach
	public void setup() {
		filter.license = Mockito.mock(LicenseHolder.class);
		filter.resourceInfo = Mockito.mock(ResourceInfo.class);
	}

	@Test
	@DisplayName("abort when providing expired license")
	public void testFilterWithExpiredLicense() {
		Mockito.doReturn(true).when(filter.license).isExpired();

		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Nested
	@DisplayName("when accessing non-annotated method")
	public class WithUnannotatedMethod {

		class TestResource {
			public void testMethod() {}
		}

		@BeforeEach
		public void setup() throws NoSuchMethodException {
			Mockito.doReturn(false).when(filter.license).isExpired();
			Mockito.doReturn(TestResource.class).when(filter.resourceInfo).getResourceClass();
			Mockito.doReturn(TestResource.class.getMethod("testMethod")).when(filter.resourceInfo).getResourceMethod();
		}

		@Test
		@DisplayName("proceed when providing no claims")
		public void testFilterWithNoClaims() {
			filter.filter(context);

			Mockito.verifyNoInteractions(context);
		}

	}

	@Nested
	@DisplayName("when accessing annotated method")
	public class WithMethodRequiringFooClaim {

		class TestResource {
			@LicenseCheck(claims = {LicenseClaim.FOO})
			public void testMethod() {}
		}

		@BeforeEach
		public void setup() throws NoSuchMethodException {
			Mockito.doReturn(false).when(filter.license).isExpired();
			Mockito.doReturn(TestResource.class).when(filter.resourceInfo).getResourceClass();
			Mockito.doReturn(TestResource.class.getMethod("testMethod")).when(filter.resourceInfo).getResourceMethod();
		}

		@Test
		@DisplayName("abort when providing no claims")
		public void testFilterWithNoClaims() {
			filter.filter(context);

			Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
		}

		@Test
		@DisplayName("proceed when providing all claims")
		public void testFilterWithAllClaims() {
			Mockito.doReturn(EnumSet.allOf(LicenseClaim.class)).when(filter.license).getClaims();

			filter.filter(context);

			Mockito.verifyNoInteractions(context);
		}

	}

	@Nested
	@DisplayName("when accessing method in annotated class")
	public class WithClassRequiringFooClaim {

		@LicenseCheck(claims = {LicenseClaim.FOO})
		class TestResource {
			public void testMethod() {}
		}

		@BeforeEach
		public void setup() throws NoSuchMethodException {
			Mockito.doReturn(false).when(filter.license).isExpired();
			Mockito.doReturn(TestResource.class).when(filter.resourceInfo).getResourceClass();
			Mockito.doReturn(TestResource.class.getMethod("testMethod")).when(filter.resourceInfo).getResourceMethod();
		}

		@Test
		@DisplayName("abort when providing no claims")
		public void testFilterWithNoClaims() {
			filter.filter(context);

			Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
		}

		@Test
		@DisplayName("proceed when providing all claims")
		public void testFilterWithAllClaims() {
			Mockito.doReturn(EnumSet.allOf(LicenseClaim.class)).when(filter.license).getClaims();

			filter.filter(context);

			Mockito.verifyNoInteractions(context);
		}

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