package org.cryptomator.hub.license;

import org.cryptomator.hub.entities.User;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
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

	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		long usedSeats = User.countEffectiveVaultUsers();
		if (usedSeats >= license.getSeats()) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}
}
