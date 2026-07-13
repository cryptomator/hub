package org.cryptomator.hub.filters;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.cryptomator.hub.license.LicenseHolder;

/**
 * Global request filter which rejects requests with status 402 while no license is configured yet ("setup mode"),
 * unless the matched resource method or class is annotated with {@link AvailableDuringSetup}.
 */
@Provider
@Priority(Priorities.USER) // runs after authentication
public class LicenseSetupFilter implements ContainerRequestFilter {

	private final LicenseHolder license;
	private final ResourceInfo resourceInfo; // @RequestScoped bean, injected as a client proxy resolving against the current request

	@Inject
	LicenseSetupFilter(LicenseHolder license, ResourceInfo resourceInfo) {
		this.license = license;
		this.resourceInfo = resourceInfo;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (license.isSetupRequired() && !isAvailableDuringSetup(resourceInfo)) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}

	private static boolean isAvailableDuringSetup(ResourceInfo resourceInfo) {
		var method = resourceInfo.getResourceMethod();
		var clazz = resourceInfo.getResourceClass();
		return (method != null && method.isAnnotationPresent(AvailableDuringSetup.class))
				|| (clazz != null && clazz.isAnnotationPresent(AvailableDuringSetup.class));
	}

}
