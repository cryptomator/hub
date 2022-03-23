package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.jose4j.base64url.Base64;

import javax.enterprise.context.ApplicationScoped;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@ApplicationScoped
public class LicenseValidator {

	private static final String LICENSE_PUBLIC_KEY = """
			MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBjvVwj5K4/v6yq23luaEEYYG9ru6z\
			BuXeQLtZNy49FlGA5rbeumoruFVQfVPuV8R9mofxyJBpU4ixi8KGkYl+eEQBTGvN\
			EQ9Z36gBX2uZOCOfHM4x50lpwtTZ0QA3B07WPhmvupy9gZk18NHuysOd8KZFEPpG\
			YGmYBhMZXAL30qweiBQ=
			""";

	private final JWTVerifier verifier;

	public LicenseValidator() {
		var algorithm = Algorithm.ECDSA512(decodePublicKey(LICENSE_PUBLIC_KEY), null);
		this.verifier = JWT.require(algorithm).build();
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

	public DecodedJWT validate(String token) throws JWTVerificationException {
		return verifier.verify(token);
	}

}
