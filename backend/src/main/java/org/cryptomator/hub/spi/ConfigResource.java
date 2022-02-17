package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.StringJoiner;
import java.util.StringTokenizer;

@Path("/config")
public class ConfigResource {

	private static final String KC_REALM_DELIM = "/realms/";

	@Inject
	@ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "")
	String oidcUrl;

	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		int delimPos = oidcUrl.indexOf(KC_REALM_DELIM);
		var kcBaseUrl = oidcUrl.substring(0, delimPos);
		var kcRealmName = oidcUrl.substring(delimPos + KC_REALM_DELIM.length());
		return new ConfigDto(kcBaseUrl, kcRealmName);
	}

	public record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl, @JsonProperty("keycloakRealm") String keycloakRealm) {
	}

}
