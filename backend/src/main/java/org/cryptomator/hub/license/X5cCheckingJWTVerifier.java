package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

class X5cCheckingJWTVerifier implements JWTVerifier {

	private final X509Certificate trustedRootCertificate;
	private final JWTVerifier fallback;
	private final String expectedIntermediateCn;

	X5cCheckingJWTVerifier(X509Certificate trustedRootCertificate, JWTVerifier fallback, String expectedIntermediateCn) {
		this.trustedRootCertificate = Objects.requireNonNull(trustedRootCertificate);
		this.fallback = fallback;
		this.expectedIntermediateCn = Objects.requireNonNull(expectedIntermediateCn);
	}

	@Override
	public DecodedJWT verify(String token) throws JWTVerificationException {
		return verify(JWT.decode(token));
	}

	@Override
	public DecodedJWT verify(DecodedJWT decodedJWT) throws JWTVerificationException {
		var x5cChain = decodedJWT.getHeaderClaim("x5c").asList(String.class);
		if (x5cChain == null || x5cChain.isEmpty()) {
			// if present, the x5c claim must contain leaf and intermediate certificate.
			// If not present or invalid, we fall back to the old verification method (e.g., for tokens that are signed with the old private key and thus do not contain the x5c claim).
			return fallback.verify(decodedJWT);
		}

		var leafPublicKey = verifyCertChain(x5cChain);
		var expiresLeeway = Instant.now().getEpochSecond();
		var x5cVerifier = JWT.require(Algorithm.ECDSA512(leafPublicKey, null))
				.acceptExpiresAt(expiresLeeway) // this will make sure to accept tokens that expired in the past (beginning from 1970)
				.ignoreIssuedAt() // ignoring issued at will make sure to accept tokens that are issued "in the future" e.g. when the hub time is behind the store time
				.build();
		return x5cVerifier.verify(decodedJWT);
	}

	private ECPublicKey verifyCertChain(List<String> x5cChain) throws JWTVerificationException {
		try {
			var leafCertificate = X509Helper.validateX5cChain(x5cChain, trustedRootCertificate);
			verifyIntermediateCn(x5cChain);
			if (leafCertificate.getPublicKey() instanceof ECPublicKey leafPublicKey) {
				return leafPublicKey;
			} else {
				throw new JWTVerificationException("Unsupported key algorithm in x5c leaf certificate. Expected EC.");
			}
		} catch (GeneralSecurityException e) {
			throw new JWTVerificationException("Invalid x5c certificate chain.", e);
		}
	}

	private void verifyIntermediateCn(List<String> x5cChain) throws GeneralSecurityException {
		if (expectedIntermediateCn.isEmpty()) {
			return;
		}
		for (var encodedCert : x5cChain) {
			var cert = X509Helper.parseCertificate(encodedCert);
			var actualCn = X509Helper.getCommonName(cert);
			if (expectedIntermediateCn.equals(actualCn)) {
				return;
			}
		}
		throw new JWTVerificationException("Expected certificate CN not found in x5c chain.");
	}

}
