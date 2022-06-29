package org.cryptomator.hub.license;

import org.cryptomator.hub.entities.EffectiveVaultAccess;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Request filter which checks if the license is not expired and available seats in the license are not already exceeded.
 * <p>
 * Applied to all methods annotated with {@link NonExpiredLicense}.
 */
@Provider
@NonExpiredLicense
public class NonExpiredLicenseFilter implements ContainerRequestFilter {

	@Inject
	LicenseHolder license;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		//Problem: Nutzer kann bereits teil eines Vaults sein
		if (license.isExpired()) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}
}
