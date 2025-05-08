package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.base64url.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;

@ApplicationScoped
public class LicenseVerifierProducer {

	@Inject
	@ConfigProperty(name = "hub.license.public-key")
	String licensePublicKey;

	@Produces
	@ApplicationScoped
	@Named("licenseVerifier")
	public JWTVerifier produceLicenseVerifier() {
		var algorithm = Algorithm.ECDSA512(decodePublicKey(licensePublicKey), null);
		var expiresleeway = Instant.now().getEpochSecond(); // this will make sure to accept tokens that expired in the past (beginning from 1970)
		// ignoring issued at will make sure to accept tokens that are issued "in the future" e.g. when the hub time is behind the store time
		return JWT.require(algorithm).acceptExpiresAt(expiresleeway).ignoreIssuedAt().build();
	}

	private static ECPublicKey decodePublicKey(String pemEncodedPublicKey) {
		try {
			var keyBytes = Base64.decode(pemEncodedPublicKey);
			var key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
			if (key instanceof ECPublicKey k) {
				return k;
			} else {
				throw new IllegalStateException("Key not an EC public key.");
			}
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("Invalid license public key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
