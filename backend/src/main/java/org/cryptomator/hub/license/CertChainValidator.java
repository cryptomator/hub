package org.cryptomator.hub.license;

import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class CertChainValidator {

	private CertChainValidator() {
	}

	public static X509Certificate validateX5cChain(List<String> x5c, X509Certificate trustedRootCertificate) throws GeneralSecurityException {
		if (x5c == null || x5c.isEmpty()) {
			throw new IllegalArgumentException("x5c header must contain at least one certificate.");
		}
		if (trustedRootCertificate == null) {
			throw new IllegalArgumentException("Trusted root certificate must not be null.");
		}

		var chain = new ArrayList<>(decodeCertificates(x5c));
		for (var cert : chain) {
			cert.checkValidity();
		}

		// JWS x5c is leaf-first. Remove trust anchor if present at the end.
		if (!chain.isEmpty() && chain.getLast().equals(trustedRootCertificate)) {
			chain.removeLast();
		}
		if (chain.isEmpty()) {
			throw new IllegalArgumentException("x5c chain does not contain a leaf certificate.");
		}

		var certPath = CertificateFactory.getInstance("X.509").generateCertPath(chain);
		var pkixParams = new PKIXParameters(Set.of(new TrustAnchor(trustedRootCertificate, null)));
		pkixParams.setDate(new Date());
		pkixParams.setRevocationEnabled(false);
		CertPathValidator.getInstance("PKIX").validate(certPath, pkixParams);

		return chain.getFirst();
	}

	public static X509Certificate decodeCertificate(String encodedCertificate) throws GeneralSecurityException {
		var normalized = encodedCertificate.replace("-----BEGIN CERTIFICATE-----", "") //
				.replace("-----END CERTIFICATE-----", "") //
				.replaceAll("\\s+", "");
		var derBytes = Base64.getDecoder().decode(normalized);
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new java.io.ByteArrayInputStream(derBytes));
	}

	private static List<X509Certificate> decodeCertificates(List<String> encodedCertificates) throws GeneralSecurityException {
		var certs = new ArrayList<X509Certificate>(encodedCertificates.size());
		for (var encodedCert : encodedCertificates) {
			certs.add(decodeCertificate(encodedCert));
		}
		return certs;
	}
}
