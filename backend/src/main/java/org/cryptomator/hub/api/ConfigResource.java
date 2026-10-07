package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.OidcConfigurationMetadata;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
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

	/**
	 * Advertised to clients via {@link ConfigDto#apiLevel()} so they can detect the capabilities of this Hub instance.
	 * <ul>
	 *     <li>0: initial</li>
	 *     <li>1: new unlock flow via {@code /devices/{deviceId}} (Hub 1.3.0)</li>
	 *     <li>2: Hub 1.3.0 sent a response header that was missing before</li>
	 *     <li>3: HTTP 402 Payment Required on exceeded seats (Hub 1.3.4)</li>
	 *     <li>4: Hub 1.4.0</li>
	 *     <li>5: Universal Vault Format: {@code /vaults/{vaultId}/uvf/*}, access tokens may contain a UVF member key</li>
	 * </ul>
	 */
	static final int API_LEVEL = 5;

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

		return new ConfigDto(keycloakPublicUrl, keycloakRealm, keycloakClientIdHub, keycloakClientIdCryptomator, authUri, tokenUri, Instant.now().truncatedTo(ChronoUnit.MILLIS), API_LEVEL, license.getEntitlements(), billingUrl, licenseApiUrl, license.isSetupRequired());
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

	public record ConfigDto(@JsonProperty("keycloakUrl") @NotNull String keycloakUrl, @JsonProperty("keycloakRealm") @NotNull String keycloakRealm,
							@JsonProperty("keycloakClientIdHub") @NotNull String keycloakClientIdHub, @JsonProperty("keycloakClientIdCryptomator") @NotNull String keycloakClientIdCryptomator,
							@JsonProperty("keycloakAuthEndpoint") @NotNull String authEndpoint, @JsonProperty("keycloakTokenEndpoint") @NotNull String tokenEndpoint,
							@JsonProperty("serverTime") @NotNull Instant serverTime, @JsonProperty("apiLevel") @NotNull Integer apiLevel,
							@JsonProperty("entitlements") @NotNull HubLicenseEntitlements entitlements,
							@JsonProperty("billingUrl") @NotNull String billingUrl,
							@JsonProperty("licenseApiUrl") @NotNull String licenseApiUrl,
							@JsonProperty("licenseSetupRequired") boolean licenseSetupRequired) {
	}

}
