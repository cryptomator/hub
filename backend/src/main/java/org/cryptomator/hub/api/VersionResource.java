package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.SystemInfoRepresentation;

import java.util.Optional;

@Path("/version")
public class VersionResource {

	@ConfigProperty(name = "quarkus.application.version")
	String hubVersion;

	@Inject
	Keycloak keycloak;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "get version of hub and keycloak")
	@APIResponse(responseCode = "200")
	public VersionDto getVersion() {
		var keycloakVersion = Optional.ofNullable(keycloak.serverInfo().getInfo().getSystemInfo())
				.map(SystemInfoRepresentation::getVersion)
				.orElse(null);
		return new VersionDto(hubVersion, keycloakVersion);
	}

	public record VersionDto(@JsonProperty("hubVersion") String hubVersion, @JsonProperty("keycloakVersion") String keycloakVersion) {
	}

}
