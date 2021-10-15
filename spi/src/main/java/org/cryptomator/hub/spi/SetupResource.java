package org.cryptomator.hub.spi;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;

@Path("/setup")
public class SetupResource {

	private static final String DEFAULT_OIDC_URL = "http://localhost:8080";

	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	private String oidcUrl;

	@GET
	@Path("/keycloak-url")
	@Produces(MediaType.TEXT_PLAIN)
	public String keycloakUrl() {
		try {
			var url = new URL(oidcUrl);
			return url.getProtocol() + "://" + url.getAuthority();
		} catch (MalformedURLException e) {
			return DEFAULT_OIDC_URL;
		}
	}
}
