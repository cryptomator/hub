package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.config.Config;
import org.cryptomator.hub.spi.keycloak.AuthService;
import org.cryptomator.hub.spi.keycloak.RealmsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Path("/setup")
public class SetupResource {

	private static final String DEFAULT_OIDC_URL = "http://localhost:8080";
	private static final String GRANT_TYPE_PW = "password";
	private static final String ADMIN_CLIENT_ID = "admin-cli";

	@Inject
	Config config; // TODO: replace, once we have a writable config

	// TODO: verify this still works with custom writable config
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	private String oidcUrl;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		return new ConfigDto(keycloakUrl());
	}

	private String keycloakUrl() {
		try {
			var url = new URL(oidcUrl);
			return url.getProtocol() + "://" + url.getAuthority();
		} catch (MalformedURLException e) {
			return DEFAULT_OIDC_URL;
		}
	}

	@AppSetup
	@POST
	@Path("/create-realm")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public Response createRealm(@FormParam("kcUrl") String url, @FormParam("user") String user, @FormParam("password") String password, @FormParam("realmCfg") String realmCfg) {
		try {
			var baseUrl = new URL(url);
			var token = auth(baseUrl, user, password);
			createRealm(baseUrl, token, realmCfg);
			config.setSetup(true);
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

	private void createRealm(URL url, String accessToken, String realmCfg) {
		try (var in = new ByteArrayInputStream(realmCfg.getBytes(StandardCharsets.UTF_8))) {
			var realmsService = RestClientBuilder.newBuilder()
					.baseUrl(url)
					.build(RealmsService.class);
			var authHeader = "Bearer " + accessToken;
			realmsService.importRealm(authHeader, in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl) {
	}

}
