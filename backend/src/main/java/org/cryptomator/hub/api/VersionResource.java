package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.SyncerConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.admin.client.Keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/version")
public class VersionResource {

	@ConfigProperty(name = "quarkus.application.version")
	String hubVersion;

	@Inject
	SyncerConfig syncerConfig;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "get version of hub and keycloak")
	@APIResponse(responseCode = "200")
	public VersionDto getVersion() {
		try (Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {
			var keycloakVersion = keycloak.serverInfo().getInfo().getSystemInfo().getVersion();
			return new VersionDto(hubVersion, keycloakVersion);
		}
	}

	public record VersionDto(@JsonProperty("hubVersion") String hubVersion, @JsonProperty("keycloakVersion") String keycloakVersion) {
	}

}
