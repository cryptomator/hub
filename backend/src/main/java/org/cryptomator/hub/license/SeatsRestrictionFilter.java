package org.cryptomator.hub.license;

import org.cryptomator.hub.entities.EffectiveVaultAccess;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * TODO: docs & tests
 */
@Provider
@SeatsRestricted
public class SeatsRestrictionFilter implements ContainerRequestFilter {

	@Inject
	LicenseHolder license;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		long usedSeats = EffectiveVaultAccess.countEffectiveVaultUsers();
		if (usedSeats >= license.getAvailableSeats()) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}
}
