package org.cryptomator.hub.license;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

final class X509Helper {

	private X509Helper() {}

	/**
	 * Validates the given leaf-first certificate chain against the trusted root and returns its leaf certificate.
	 *
	 * @param x5c                    The base64 DER-encoded certificates, leaf first, as found in a JWS {@code x5c} header
	 * @param trustedRootCertificate The trust anchor the chain must lead to
	 * @param validAt                The point in time at which every certificate in the chain must be valid, i.e. the time the signature was made
	 * @return The leaf certificate
	 * @throws GeneralSecurityException If the chain can not be decoded, is not valid at the given time or does not lead to the trusted root
	 */
	public static X509Certificate validateX5cChain(List<String> x5c, X509Certificate trustedRootCertificate, Instant validAt) throws GeneralSecurityException {
		if (x5c == null || x5c.isEmpty()) {
			throw new CertPathValidatorException("null or empty x5c chain");
		}
		var validationDate = Date.from(validAt);
		var chain = new ArrayList<>(decodeCertificates(x5c));
		for (var cert : chain) {
			cert.checkValidity(validationDate);
		}

		// JWS x5c is leaf-first. Remove trust anchor if present at the end.
		if (!chain.isEmpty() && chain.getLast().equals(trustedRootCertificate)) {
			chain.removeLast();
		}
		if (chain.isEmpty()) {
			throw new CertPathValidatorException("x5c chain does not contain a leaf certificate.");
		}

		var certPath = CertificateFactory.getInstance("X.509").generateCertPath(chain);
		var pkixParams = new PKIXParameters(Set.of(new TrustAnchor(trustedRootCertificate, null)));
		pkixParams.setRevocationEnabled(false);
		pkixParams.setDate(validationDate);
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
	 * @param encoded The encoded certificate (PEM or base64 DER)
	 * @return The decoded X.509 certificate
	 * @throws CertificateException In case of invalid input
	 */
	public static X509Certificate parseCertificate(String encoded) throws CertificateException {
		if (encoded == null || encoded.isBlank()) {
			throw new CertificateParsingException("Certificate string is null or empty.");
		}
		var certBytes = decodeBase64PemOrDer(encoded);
		var certFactory = CertificateFactory.getInstance("X.509");
		var cert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
		if (cert instanceof X509Certificate x509Cert) {
			return x509Cert;
		}
		throw new CertificateException("X.509 certificate could not be decoded");
	}

	private static byte[] decodeBase64PemOrDer(String value) throws CertificateParsingException {
		var normalized = value
				.replace("-----BEGIN CERTIFICATE-----", "")
				.replace("-----END CERTIFICATE-----", "")
				.replaceAll("\\s", "");
		try {
			return Base64.getDecoder().decode(normalized);
		} catch (IllegalArgumentException e) {
			throw new CertificateParsingException(e);
		}
	}

	public static String getCommonName(X509Certificate cert) throws CertificateException {
		try {
			var dn = cert.getSubjectX500Principal().getName(X500Principal.RFC2253);
			for (var rdn : new LdapName(dn).getRdns()) {
				if ("CN".equals(rdn.getType())) {
					return String.valueOf(rdn.getValue());
				}
			}
			throw new CertificateException("Certificate subject does not contain CN.");
		} catch (InvalidNameException e) {
			throw new CertificateException("Invalid certificate subject.", e);
		}
	}
}
