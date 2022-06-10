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
 * Applied to all methods annotated with {@link SeatsRestricted}.
 */
@Provider
@SeatsRestricted
public class SeatsRestrictionFilter implements ContainerRequestFilter {

	@Inject
	LicenseHolder license;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		long usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();

		if (license.isExpired() || (usedSeats >= license.getAvailableSeats())) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}
}
