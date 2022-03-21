package org.cryptomator.hub.license;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

@Provider
@LicenseCheck
public class LicenseFilter implements ContainerRequestFilter {

	@Inject
	LicenseHolder license;

	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (license.isExpired() || !fulfillsRequestedClaims()) {
			var response = Response.status(Response.Status.PAYMENT_REQUIRED).build();
			requestContext.abortWith(response);
		}
	}

	private boolean fulfillsRequestedClaims() {
		var licenseCheck = resourceInfo.getResourceMethod().getAnnotation(LicenseCheck.class);
		if (licenseCheck == null) {
			licenseCheck = resourceInfo.getResourceClass().getAnnotation(LicenseCheck.class);
		}
		if (licenseCheck == null) {
			return true; // no claims requested
		} else {
			var requestedClaims = licenseCheck.claims();
			var providedClaims = license.getClaims();
			return providedClaims.containsAll(Arrays.asList(requestedClaims));
		}
	}
}
