package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.security.cert.X509Certificate;

class X5cCheckingJWTVerifier implements JWTVerifier {

	private final JWTVerifier legacyVerifier;
	private final X509Certificate trustedRootCertificate;
	private final long expiresLeeway;

	X5cCheckingJWTVerifier(JWTVerifier legacyVerifier, X509Certificate trustedRootCertificate, long expiresLeeway) {
		this.legacyVerifier = legacyVerifier;
		this.trustedRootCertificate = trustedRootCertificate;
		this.expiresLeeway = expiresLeeway;
	}

	@Override
	public DecodedJWT verify(String token) throws JWTVerificationException {
		return verify(JWT.decode(token));
	}

	@Override
	public DecodedJWT verify(DecodedJWT decodedJWT) throws JWTVerificationException {
		var x5cChain = decodedJWT.getHeaderClaim("x5c").asList(String.class);
		if (x5cChain == null || x5cChain.isEmpty()) {
			return legacyVerifier.verify(decodedJWT);
		}
		if (trustedRootCertificate == null) {
			throw new JWTVerificationException("Token contains x5c header, but no trusted root certificate is configured.");
		}

		try {
			var leafCertificate = CertChainValidator.validateX5cChain(x5cChain, trustedRootCertificate);
			if (!(leafCertificate.getPublicKey() instanceof ECPublicKey leafPublicKey)) {
				throw new JWTVerificationException("Unsupported key algorithm in x5c leaf certificate. Expected EC.");
			}
			var x5cVerifier = JWT.require(Algorithm.ECDSA512(leafPublicKey, null)).acceptExpiresAt(expiresLeeway).ignoreIssuedAt().build();
			return x5cVerifier.verify(decodedJWT);
		} catch (GeneralSecurityException e) {
			throw new JWTVerificationException("Invalid x5c certificate chain.", e);
		}
	}
}
