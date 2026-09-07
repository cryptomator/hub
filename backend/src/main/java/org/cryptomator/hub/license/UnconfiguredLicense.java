package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Placeholder license used while no real license is configured ("setup mode").
 * <p>
 * The token is unsigned, only exists in memory (never persisted to the database) and grants no seats. Its expiration date lies in the year 3000, so
 * {@link LicenseHolder#isExpired()} stays {@code false} — API access during setup mode is instead restricted by {@code LicenseSetupFilter}.
 */
public final class UnconfiguredLicense {

	private static final String HEADER = """
			{"alg":"none","typ":"JWT"} \
			""";
	// iat 0 = epoch, exp 32503680000 = 3000-01-01T00:00:00Z; entitlements mirror HubLicenseEntitlements.create()
	private static final String PAYLOAD_TEMPLATE = """
			{
				"jti": "%s",
				"iss": "Cryptomator Hub",
				"sub": "unconfigured@localhost",
				"iat": 0,
				"exp": 32503680000,
				"seats": 0,
				"org.cryptomator.hub.entitlements": {
					"seats": 0,
					"showTrialHint": false,
					"auditLogRetentionDays": 0,
					"emergencyAccessEnabled": false,
					"keycloakAccessEnabled": true,
					"iosLicense": null,
					"androidLicense": null,
					"desktopLicense": null
				}
			}""";

	private UnconfiguredLicense() {
	}

	/**
	 * Builds the placeholder license for this Hub instance.
	 *
	 * @param hubId the instance's hub ID, becoming the token's {@code jti} claim (displayed to admins during setup, e.g. in store links)
	 * @return the decoded, unsigned placeholder token
	 */
	public static DecodedJWT create(String hubId) {
		var encoder = Base64.getUrlEncoder().withoutPadding();
		var header = encoder.encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));
		var payload = encoder.encodeToString(PAYLOAD_TEMPLATE.formatted(hubId).getBytes(StandardCharsets.UTF_8));
		return JWT.decode(header + "." + payload + "."); // unsigned, decode-only — never passes through LicenseValidator
	}

}
