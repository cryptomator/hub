package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.config.HubConfig;
import org.cryptomator.hub.spi.keycloak.AdminClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Path("/setup")
public class SetupResource {

	private static final Logger LOG = Logger.getLogger(SetupResource.class);

	@Inject
	HubConfig config;

	@Inject
	@ConfigProperty(name = "hub.setupCompleted", defaultValue = "false")
	Provider<Boolean> setupCompleted;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "")
	Provider<String> oidcUrl;

	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		return new ConfigDto(setupCompleted.get(), oidcUrl.get());
	}

	@PermitAll
	@AppSetup
	@POST
	@Path("/create-realm")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public Response createRealm(@FormParam("kcUrl") String url, @FormParam("user") String user, @FormParam("password") String password, @FormParam("realmCfg") String realmCfg) {
		try {
			var baseUrl = new URL(url);
			AdminClient.connect(baseUrl, user, password).createRealm(realmCfg);
			config.setSetupCompleted(true);
			config.setOidcAuthEndpoint(url);
			return Response.noContent().build();
		} catch (MalformedURLException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		} catch (IOException e) {
			LOG.error("Failed to create realm", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	public static record ConfigDto(@JsonProperty("setupCompleted") boolean setupCompleted,
								   @JsonProperty("keycloakUrl") String keycloakUrl) {
	}

}
