package org.cryptomator.hub.filters;

import org.cryptomator.hub.license.LicenseHolder;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

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
