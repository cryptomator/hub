package org.cryptomator.hub.spi;

import org.cryptomator.hub.config.Config;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@AppSetup
public class AppSetupFilter implements ContainerRequestFilter {

	@Inject
	Config config;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (config.isSetup()) {
			var response = Response.status(Response.Status.CONFLICT).build();
			requestContext.abortWith(response);
		}
	}
}
