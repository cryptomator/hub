package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/config")
public class ConfigResource {

	@Inject
	@ConfigProperty(name = "hub.keycloak.public-url", defaultValue = "")
	String keycloakUrl;

	@Inject
	@ConfigProperty(name = "hub.keycloak.realm", defaultValue = "")
	String keycloakRealm;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "")
	String keycloakClientId;

	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		return new ConfigDto(keycloakUrl, keycloakRealm, keycloakClientId);
	}

	public record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl, @JsonProperty("keycloakRealm") String keycloakRealm, @JsonProperty("keycloakClientId") String keycloakClientId) {
	}

}
