package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@ApplicationScoped
public class LicenseVerifierProducer {

	@Deprecated // TODO: once all issued tokens contain the x5c claim, we can remove the legacy verification method
	private static final String LICENSE_PUBLIC_KEY = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBjvVwj5K4/v6yq23luaEEYYG9ru6zBuXeQLtZNy49FlGA5rbeumoruFVQfVPuV8R9mofxyJBpU4ixi8KGkYl+eEQBTGvNEQ9Z36gBX2uZOCOfHM4x50lpwtTZ0QA3B07WPhmvupy9gZk18NHuysOd8KZFEPpGYGmYBhMZXAL30qweiBQ=";
	private static final String LICENSE_ROOT_CERTIFICATE = """
			-----BEGIN CERTIFICATE-----
			MIIBqDCCAVqgAwIBAgIUKNImuR2JD+NyAWaYzb8V8w8SdFwwBQYDK2VwMD8xCzAJ
			BgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNl
			bnNlIFJvb3QgQ0EwIBcNMjYwMjI2MTMzMTQxWhgPMjA3NjAyMTQxMzMxNDFaMD8x
			CzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9M
			aWNlbnNlIFJvb3QgQ0EwKjAFBgMrZXADIQCOyUIv+3Ust66VWJ8yH5ruJGxyZC1u
			LK2Yxb+ZtPGAQKNmMGQwEgYDVR0TAQH/BAgwBgEB/wIBATAOBgNVHQ8BAf8EBAMC
			AQYwHQYDVR0OBBYEFGhKUh0jCta2wXzxrUldIBqB4Bz3MB8GA1UdIwQYMBaAFGhK
			Uh0jCta2wXzxrUldIBqB4Bz3MAUGAytlcANBAOERjFKpGnjxH1nh2u5lsCjX65zz
			XisC7XFaZQikVLKzHK+YTIusi3x7dGCFBjO/m3ieQpt7BsaPo0lLL719pQ8=
			-----END CERTIFICATE-----
			""";

	private final String licenseChainRequiredCn;

	@Inject
	LicenseVerifierProducer(@ConfigProperty(name = "hub.license.chain.required-cn") String licenseChainRequiredCn) {
		this.licenseChainRequiredCn = licenseChainRequiredCn;
	}

	@Produces
	@ApplicationScoped
	@Named("licenseVerifier")
	public JWTVerifier produceLicenseVerifier() {
		return produceLicenseVerifier(LICENSE_ROOT_CERTIFICATE, Objects.requireNonNull(licenseChainRequiredCn));
	}

	// visible for testing
	JWTVerifier produceLicenseVerifier(String rootCert, String licenseChainRequiredCn) throws JWTVerificationException {
		var fallback = produceLegacyVerifier();
		try {
			var trustedRoot = X509Helper.parseCertificate(rootCert);
			return new X5cCheckingJWTVerifier(trustedRoot, fallback, licenseChainRequiredCn);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Invalid trusted root certificate", e);
		}
	}

	@Deprecated // TODO: once all issued tokens contain the x5c claim, we can remove the legacy verification method
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
