package org.cryptomator.hub.filters;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.cryptomator.hub.license.LicenseHolder;

/**
 * Request filter which checks if the license is not expired.
 * <p>
 * Applied to all methods annotated with {@link ActiveLicense}.
 */
@Provider
@ActiveLicense
public class ActiveLicenseFilter implements ContainerRequestFilter {

	@Inject
	LicenseHolder license;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (license.isExpired()) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}
}
