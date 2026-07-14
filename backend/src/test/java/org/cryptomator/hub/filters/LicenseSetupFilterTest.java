package org.cryptomator.hub.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.license.LicenseHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.lang.reflect.Method;

public class LicenseSetupFilterTest {

	ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
	LicenseHolder license = Mockito.mock(LicenseHolder.class);
	ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
	LicenseSetupFilter filter = new LicenseSetupFilter(license, resourceInfo);

	@Test
	@DisplayName("abort with 402 when setup is required and resource is not allowlisted")
	void testFilterInSetupMode() throws NoSuchMethodException {
		Mockito.doReturn(true).when(license).isSetupRequired();
		mockMatchedResource(UnannotatedResource.class, "unannotatedMethod");

		filter.filter(context);

		Mockito.verify(context).abortWith(Mockito.argThat(StatusCodeMatcher.hasStatus(Response.Status.PAYMENT_REQUIRED)));
	}

	@Test
	@DisplayName("continue when setup is required but the resource method is annotated")
	void testDontFilterAnnotatedMethod() throws NoSuchMethodException {
		Mockito.doReturn(true).when(license).isSetupRequired();
		mockMatchedResource(UnannotatedResource.class, "annotatedMethod");

		filter.filter(context);

		Mockito.verifyNoInteractions(context);
	}

	@Test
	@DisplayName("continue when setup is required but the resource class is annotated")
	void testDontFilterAnnotatedClass() throws NoSuchMethodException {
		Mockito.doReturn(true).when(license).isSetupRequired();
		mockMatchedResource(AnnotatedResource.class, "unannotatedMethod");

		filter.filter(context);

		Mockito.verifyNoInteractions(context);
	}

	@Test
	@DisplayName("continue when no setup is required")
	void testDontFilterWithConfiguredLicense() {
		Mockito.doReturn(false).when(license).isSetupRequired();

		filter.filter(context);

		Mockito.verifyNoInteractions(context);
	}

	private void mockMatchedResource(Class<?> clazz, String methodName) throws NoSuchMethodException {
		Method method = clazz.getDeclaredMethod(methodName);
		Mockito.doReturn(clazz).when(resourceInfo).getResourceClass();
		Mockito.doReturn(method).when(resourceInfo).getResourceMethod();
	}

	private static class UnannotatedResource {
		@SuppressWarnings("unused")
		public void unannotatedMethod() {
		}

		@AvailableDuringSetup
		@SuppressWarnings("unused")
		public void annotatedMethod() {
		}
	}

	@AvailableDuringSetup
	private static class AnnotatedResource {
		@SuppressWarnings("unused")
		public void unannotatedMethod() {
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
