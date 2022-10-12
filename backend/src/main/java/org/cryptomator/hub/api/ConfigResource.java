package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.OidcConfigurationMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Path("/config")
public class ConfigResource {

	@Inject
	@ConfigProperty(name = "hub.keycloak.public-url", defaultValue = "")
	String keycloakPublicUrl;

	@Inject
	@ConfigProperty(name = "hub.keycloak.realm", defaultValue = "")
	String keycloakRealm;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "")
	String keycloakClientId;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String internalRealmUrl;

	@Inject
	OidcConfigurationMetadata oidcConfData;


	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		var publicRealmUri = trimTrailingSlash(keycloakPublicUrl + "/realms/" + keycloakRealm);
		var authUri = replacePrefix(oidcConfData.getAuthorizationUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);
		var tokenUri = replacePrefix(oidcConfData.getTokenUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);

		return new ConfigDto(keycloakPublicUrl, keycloakRealm, keycloakClientId, authUri, tokenUri, ZonedDateTime.now(ZoneOffset.UTC));
	}

	//visible for testing
	String replacePrefix(String str, String prefix, String replacement) {
		int index = str.indexOf(prefix);
		if (index == 0) {
			return replacement + str.substring(prefix.length());
		} else {
			return str;
		}
	}

	//visible for testing
	String trimTrailingSlash(String str) {
		if (str.endsWith("/")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}

	}

	public record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl, @JsonProperty("keycloakRealm") String keycloakRealm,
							@JsonProperty("keycloakClientId") String keycloakClientId, @JsonProperty("keycloakAuthEndpoint") String authEndpoint,
							@JsonProperty("keycloakTokenEndpoint") String tokenEndpoint, @JsonProperty("serverTime") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") ZonedDateTime serverTime) {
	}

}
