package org.cryptomator.hub.license;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

final class X509Helper {

	private X509Helper() {}

	public static X509Certificate validateX5cChain(List<String> x5c, X509Certificate trustedRootCertificate) throws GeneralSecurityException {
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
		pkixParams.setRevocationEnabled(false);
		CertPathValidator.getInstance("PKIX").validate(certPath, pkixParams);

		return chain.getFirst();
	}

	private static List<X509Certificate> decodeCertificates(List<String> encodedCertificates) throws GeneralSecurityException {
		var certs = new ArrayList<X509Certificate>(encodedCertificates.size());
		for (var encodedCert : encodedCertificates) {
			certs.add(parseCertificate(encodedCert));
		}
		return certs;
	}

	/**
	 * Imports an X.509 certificate from the given string.
	 *
	 * @param x509Key The encoded certificate (PEM or base64 DER)
	 * @return The decoded X.509 certificate
	 * @throws CertificateException In case of invalid input
	 */
	public static X509Certificate parseCertificate(String x509Key) throws CertificateException {
		var keyBytes = decodeBase64PemOrDer(x509Key);
		var certFactory = CertificateFactory.getInstance("X.509");
		var cert = certFactory.generateCertificate(new ByteArrayInputStream(keyBytes));
		if (cert instanceof X509Certificate x509Cert) {
			return x509Cert;
		}
		throw new CertificateException("X.509 certificate could not be decoded");
	}

	private static byte[] decodeBase64PemOrDer(String value) {
		var normalized = value
				.replace("-----BEGIN CERTIFICATE-----", "")
				.replace("-----END CERTIFICATE-----", "")
				.replaceAll("\\s", "");
		return Base64.getDecoder().decode(normalized);
	}
}
