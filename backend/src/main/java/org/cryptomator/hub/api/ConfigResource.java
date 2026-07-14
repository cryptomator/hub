package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.OidcConfigurationMetadata;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.filters.AvailableDuringSetup;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Path("/config")
@AvailableDuringSetup
public class ConfigResource {

	private final String keycloakPublicUrl;
	private final String keycloakRealm;
	private final String keycloakClientIdHub;
	private final String keycloakClientIdCryptomator;
	private final String internalRealmUrl;
	private final String billingUrl;
	private final String licenseApiUrl;
	private final OidcConfigurationMetadata oidcConfData;
	private final LicenseHolder license;

	@Inject
	ConfigResource(@ConfigProperty(name = "hub.keycloak.public-url", defaultValue = "") String keycloakPublicUrl,
				   @ConfigProperty(name = "hub.keycloak.realm", defaultValue = "") String keycloakRealm,
				   @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "") String keycloakClientIdHub,
				   @ConfigProperty(name = "hub.keycloak.oidc.cryptomator-client-id", defaultValue = "") String keycloakClientIdCryptomator,
				   @ConfigProperty(name = "quarkus.oidc.auth-server-url") String internalRealmUrl,
				   @ConfigProperty(name = "hub.billing-url", defaultValue = "") String billingUrl,
				   @ConfigProperty(name = "quarkus.rest-client.license-api.url", defaultValue = "") String licenseApiUrl,
				   OidcConfigurationMetadata oidcConfData,
				   LicenseHolder license) {
		this.keycloakPublicUrl = keycloakPublicUrl;
		this.keycloakRealm = keycloakRealm;
		this.keycloakClientIdHub = keycloakClientIdHub;
		this.keycloakClientIdCryptomator = keycloakClientIdCryptomator;
		this.internalRealmUrl = internalRealmUrl;
		this.billingUrl = billingUrl;
		this.licenseApiUrl = licenseApiUrl;
		this.oidcConfData = oidcConfData;
		this.license = license;
	}

	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		var publicRealmUri = trimTrailingSlash(keycloakPublicUrl + "/realms/" + keycloakRealm);
		var authUri = replacePrefix(oidcConfData.getAuthorizationUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);
		var tokenUri = replacePrefix(oidcConfData.getTokenUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);

		return new ConfigDto(keycloakPublicUrl, keycloakRealm, keycloakClientIdHub, keycloakClientIdCryptomator, authUri, tokenUri, Instant.now().truncatedTo(ChronoUnit.MILLIS), 4, license.getEntitlements(), billingUrl, licenseApiUrl, license.isSetupRequired());
	}

	//visible for testing
	static String replacePrefix(String str, String prefix, String replacement) {
		int index = str.indexOf(prefix);
		if (index == 0) {
			return replacement + str.substring(prefix.length());
		} else {
			return str;
		}
	}

	//visible for testing
	static String trimTrailingSlash(String str) {
		if (str.endsWith("/")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}

	}

	public record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl, @JsonProperty("keycloakRealm") String keycloakRealm,
							@JsonProperty("keycloakClientIdHub") String keycloakClientIdHub, @JsonProperty("keycloakClientIdCryptomator") String keycloakClientIdCryptomator,
							@JsonProperty("keycloakAuthEndpoint") String authEndpoint, @JsonProperty("keycloakTokenEndpoint") String tokenEndpoint,
							@JsonProperty("serverTime") Instant serverTime, @JsonProperty("apiLevel") Integer apiLevel,
							@JsonProperty("entitlements") HubLicenseEntitlements entitlements,
							@JsonProperty("billingUrl") String billingUrl,
							@JsonProperty("licenseApiUrl") String licenseApiUrl,
							@JsonProperty("licenseSetupRequired") boolean licenseSetupRequired) {
	}

}
