package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.config.Config;
import org.cryptomator.hub.spi.keycloak.AdminClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

@Path("/setup")
public class SetupResource {

	private static final String DEFAULT_OIDC_URL = "http://localhost:8080/auth";

	@Inject
	Config config; // TODO: replace, once we have a writable config

	// TODO: verify this still works with custom writable config
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String oidcUrl;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		return new ConfigDto(config.isSetup(), keycloakUrl());
	}

	private String keycloakUrl() {
		try {
			var url = new URL(oidcUrl);
			return url.getProtocol() + "://" + url.getAuthority() + "/auth";
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
			AdminClient.connect(baseUrl, user, password).createRealm(realmCfg);
			config.setSetup(true);
			return Response.noContent().build();
		} catch (MalformedURLException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	public static record ConfigDto(@JsonProperty("setupCompleted") boolean setupCompleted,
								   @JsonProperty("keycloakUrl") String keycloakUrl) {
	}

}
