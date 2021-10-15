package org.cryptomator.hub.spi;

import org.cryptomator.hub.spi.keycloak.AuthService;
import org.cryptomator.hub.spi.keycloak.RealmsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

@Path("/setup")
public class SetupResource {

	private static final String DEFAULT_OIDC_URL = "http://localhost:8080";
	private static final String GRANT_TYPE_PW = "password";
	private static final String ADMIN_CLIENT_ID = "admin-cli";

	// TODO: write this to custom hub-config.json/yaml/whatever once setup is done
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	private String oidcUrl;

	// TODO: refactor this to a global "GET /setup/config" resource used by the frontend
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

	// TODO: add a ContainerRequestFilter to prevent running this on already-configured servers
	@POST
	@Path("/create-realm")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public Response createRealm(@FormParam("kcurl") String url, @FormParam("user") String user, @FormParam("password") String password) {
		try {
			var baseUrl = new URL(url);
			var token = auth(baseUrl, user, password);
			createRealm(baseUrl, token);
			return Response.noContent().build();
		} catch (MalformedURLException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	private String auth(URL url, String user, String password) {
		var authService = RestClientBuilder.newBuilder()
				.baseUrl(url)
				.build(AuthService.class);
		return authService.authorize(GRANT_TYPE_PW, ADMIN_CLIENT_ID, user, password).accessToken;
	}

	private void createRealm(URL url, String accessToken) {
		try (var in = getClass().getResourceAsStream("/keycloak/hub-realm.json")) {
			var realmsService = RestClientBuilder.newBuilder()
					.baseUrl(url)
					.build(RealmsService.class);
			var authHeader = "Bearer " + accessToken;
			realmsService.importRealm(authHeader, in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
