package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Objects;

@ApplicationScoped
public class LicenseValidator {

	private static final String[] REQUIRED_CLAIMS = {"seats"};

	@Inject
	@Named("licenseVerifier")
	JWTVerifier verifier;

	/**
	 * Validates the token signature and whether it matches the Hub ID. It does NOT check the expiration date, though.
	 * @param token JWT
	 * @param expectedHubId the ID of this Hub instance
	 * @return the verified token.
	 * @throws JWTVerificationException If validation fails.
	 */
	public DecodedJWT validate(String token, String expectedHubId) throws JWTVerificationException {
		var jwt = verifier.verify(token);
		if (!jwt.getId().equals(expectedHubId)) {
			throw new InvalidClaimException("Token ID " + jwt.getId() + " does not match your Hub ID " + expectedHubId);
		}
		for (var claim : REQUIRED_CLAIMS) {
			if (Objects.isNull(jwt.getClaim(claim))) {
				throw new InvalidClaimException("The claim " + claim + " is required, but not present.");
			}
		}
		return jwt;
	}
}
