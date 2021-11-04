package org.cryptomator.hub.spi;

import org.cryptomator.hub.config.HubConfig;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@AppSetup
public class AppSetupFilter implements ContainerRequestFilter {

	@Inject
	HubConfig config;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (config.isSetupCompleted()) {
			var response = Response.status(Response.Status.CONFLICT).build();
			requestContext.abortWith(response);
		}
	}
}
