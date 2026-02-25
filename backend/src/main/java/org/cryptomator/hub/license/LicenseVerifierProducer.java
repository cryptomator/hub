package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class LicenseVerifierProducer {

	private static final String LICENSE_PUBLIC_KEY = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBjvVwj5K4/v6yq23luaEEYYG9ru6zBuXeQLtZNy49FlGA5rbeumoruFVQfVPuV8R9mofxyJBpU4ixi8KGkYl+eEQBTGvNEQ9Z36gBX2uZOCOfHM4x50lpwtTZ0QA3B07WPhmvupy9gZk18NHuysOd8KZFEPpGYGmYBhMZXAL30qweiBQ=";
	private static final String LICENSE_ROOT_CERTIFICATE = """
			-----BEGIN CERTIFICATE-----
			MIICbjCCAc+gAwIBAgIURZ67qQ5rU7TeUqM7MxYYo7BD1oYwCgYIKoZIzj0EAwIw
			PzELMAkGA1UEBhMCREUxFjAUBgNVBAoMDVNreW1hdGljIEdtYkgxGDAWBgNVBAMM
			D0xpY2Vuc2UgUm9vdCBDQTAeFw0yNjAyMjUxMjU3MDlaFw0zNjAyMjMxMjU3MDla
			MD8xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQD
			DA9MaWNlbnNlIFJvb3QgQ0EwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABAFc5Rp8
			bYu2WAemYVoKJ4HVoZWIcvTgryEXruBZ4cn6M2Gj63D0RFVbOF5dLiREoI9PWQoF
			aPABZBXeuQAJ3U6aBQGyOJSZVzhxhcBlDziR5PjQ4pr90Q3Vu61C+0YolQwhygsw
			X7jhnWqvTsREVjLGWpqpoEg1XYExtnW5U8or6aTW7aNmMGQwEgYDVR0TAQH/BAgw
			BgEB/wIBATAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFA+EvJM3z8sVrpLUUxEH
			Bts9PEyKMB8GA1UdIwQYMBaAFA+EvJM3z8sVrpLUUxEHBts9PEyKMAoGCCqGSM49
			BAMCA4GMADCBiAJCAR4xXcABzWgzxsLDtqsijXM28xzb4ZopjuznIoi634MWdI5J
			GWNA8Y3TcYi69kTreDwuXYLW3xwK1OWn0B8AC5/VAkIBSx5vuLl1WfNxwnXMmiGK
			2+TXA94AIPCwRWoRngW7nVajapOVeMURoGpkZHnwVJ4Op3U0b5nGfKCKSChORmi6
			gRU=
			-----END CERTIFICATE-----
			""";

	@Produces
	@ApplicationScoped
	@Named("licenseVerifier")
	public JWTVerifier produceLicenseVerifier() {
		return produceLicenseVerifier(LICENSE_ROOT_CERTIFICATE);
	}

	// visible for testing
	JWTVerifier produceLicenseVerifier(String rootCert) throws JWTVerificationException {
		var fallback = produceLegacyVerifier();
		try {
			var trustedRoot = X509Helper.parseCertificate(rootCert);
			return new X5cCheckingJWTVerifier(trustedRoot, fallback);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Invalid trusted root certificate", e);
		}
	}

	private JWTVerifier produceLegacyVerifier() {
		var algorithm = Algorithm.ECDSA512(decodePublicKey(LICENSE_PUBLIC_KEY), null);
		var expiresleeway = Instant.now().getEpochSecond(); // this will make sure to accept tokens that expired in the past (beginning from 1970)
		return JWT.require(algorithm).acceptExpiresAt(expiresleeway).ignoreIssuedAt().build(); // ignoring issued at will make sure to accept tokens that are issued "in the future" e.g. when the hub time is behind the store time
	}

	private static ECPublicKey decodePublicKey(String pemEncodedPublicKey) {
		try {
			var keyBytes = Base64.getDecoder().decode(pemEncodedPublicKey);
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
