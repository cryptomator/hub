package org.cryptomator.hub.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

class LicenseValidatorTest {

	private static final String VALID_LEGACY_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm";
	private static final String EXPIRED_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY3NzA4MzI1OSwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6OTQ2Njg0ODAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.APQnWig9ZyT6_xRviPVs3YPTaP1w_YXTpWULgvsUpCGmGQwEmT6nl0x2jNB_jkQi93E7tr9WvipvX5DkXUOYJP3OAJjzPdN7rTX2tnXTKO8irshkcqmvt79v1E4k50YLkwP-1NIwiO_ltp5sezhLbzOVPXRag6mQfc0KvS6PiZTYGYQh";
	private static final String FUTURE_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOjQyLCJpYXQiOjE3MDEyNDkzMzEzMSwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJ0b2JpYXMuaGFnZW1hbm5Ac2t5bWF0aWMuZGUiLCJzZWF0cyI6NSwiZXhwIjoxNzIyMzg0MDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.ALd0oyPR3kgntysXp8TZ1LvmHYDiDIGlbmaq52d5wAE1V8MZ1asWvufXgL9YExXvJhFbGCnLu66XgA387rxjrxKeASL_q43ZZUEDxtm8aa7uH2VMOvdM3gXEibSHUzNwO0MRWFbeYWOc8daRNWdxgOcrpX6NcMV7vPZH7yZSEct_cqf5";
	private static final String TOKEN_WITH_INVALID_SIGNATURE = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.AbVUinMiT3J_03je8WTOIl-VdggzvoFgnOsdouAs-DLOtQzau9valrq-S6pETyi9Q18HH-EuwX49Q7m3KC0GuNBJAc9Tksulgsdq8GqwIqZqDKmG7hNmDzaQG1Dpdezn2qzv-otf3ZZe-qNOXUMRImGekfQFIuH_MjD2e8RZyww6lbZk";
	private static final String MALFORMED_TOKEN = "hello world";
	private static final String ROOT_CERTIFICATE = "MIICAzCCAWSgAwIBAgIUc8N3CFaP07z8TZ5ukicAOi8WHFkwCgYIKoZIzj0EAwQwEzERMA8GA1UEAwwISHViIFJvb3QwHhcNMjYwMjI1MDk1NjE1WhcNMzYwMjIzMDk1NjE1WjATMREwDwYDVQQDDAhIdWIgUm9vdDCBmzAQBgcqhkjOPQIBBgUrgQQAIwOBhgAEAbz+Hva0BIcJLqwgjZwnip0Tosk0MnlR47bJt4xrhQ0XNA2JjqXHnKgzRhHX1cniy+gNud0gVqKjO7HHsW2pz+7sAGV94SRB71Rfc9Yht4AzeeEE8jEU8dkO8xxhKlfYQWAaZyrY1SnfSOkoLlHYRaQi/VWDkn/OZtJv4GXwMZT7ECIGo1MwUTAdBgNVHQ4EFgQUIVAOavghCKHhi23xhzFDYv7qVCYwHwYDVR0jBBgwFoAUIVAOavghCKHhi23xhzFDYv7qVCYwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDBAOBjAAwgYgCQgCD0Ccd9U7mVGqgSCxwmnmCLnXhWamdQDwuwQlVHTc09b6sKrINkJfD1RHrWOGpA5vFQW2g18Qmq6jNNtPhufmmmQJCAZWuCn0uDmyk1T/GL8awekwm+kstyVEluZ9nGNqIhgb/MiollzkV/YFU7hA10yclpkTnNoKXo6+SXhITeYc+jHyr";
	private static final String INTERMEDIATE_CERTIFICATE = "MIICHTCCAX+gAwIBAgIUQgncgS4tXHPjh4ATnp6Ia+MCzkowCgYIKoZIzj0EAwQwEzERMA8GA1UEAwwISHViIFJvb3QwHhcNMjYwMjI1MDk1NjE1WhcNMjcwMjI1MDk1NjE1WjAbMRkwFwYDVQQDDBBIdWIgSW50ZXJtZWRpYXRlMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA334k0XJ8iQ3v2XvudYrtyIsVLKMXq91qiJ3X6wCRWV1UvBRYeVITaaDHe0ckIRFKRDbrsPR1NN5X9ULJPvjpe5YBVDnqlDjR7Vx7fWu1afUM+ffzxB9dzrPxI2JHzujQO8w07CMKmBZffJfSdEwEomjamuUlFyt5ZidM/9xF2yk+OcSjZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBT0XyiSIHLZMvlYT4C+on88Ypz3yzAfBgNVHSMEGDAWgBQhUA5q+CEIoeGLbfGHMUNi/upUJjAKBggqhkjOPQQDBAOBiwAwgYcCQXex+3p07kwiVB1dD/SiFe4/X5ywgGR7BSFmLQFL6tUTT9ZYqSulbykfooQSRsuot7HPRWbWvQSNsswHRKELug0UAkIAkFQVEeISX4Mgdxle8GbcL1xaHPdsKf+fNrbJN2zEyMXkxMaXIXtk8VQGuBza5Rq0Gg+wxWL++wSs+Ptf2I12uBw=";
	private static final String LEAF_CERTIFICATE = "MIICGjCCAXygAwIBAgIUSnXPCvi1ZRDY97lnBHyyPXO1FNEwCgYIKoZIzj0EAwQwGzEZMBcGA1UEAwwQSHViIEludGVybWVkaWF0ZTAeFw0yNjAyMjUwOTU2MTVaFw0yNjA1MjYwOTU2MTVaMBYxFDASBgNVBAMMC0h1YiBTaWduaW5nMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBsPQ0+3p7grVCgrCDxsdBX+d+n/s2LNSR3rKa8maEeMjR6lrF0p32Q1qo6hY6DfNn/bu2ofA8gkH+7GErHTWc2kMAhV0MIPWZTdmoFBXvyhXt68Mcpedn8zDwR7H3211SzW5zmRrNYz1s225wvh3CcLH5HCZnCXfxsg4YkxFzaVz2SZejYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMB0GA1UdDgQWBBSkMG5Igq7CKDc1uVQEyxcGuYoWLzAfBgNVHSMEGDAWgBT0XyiSIHLZMvlYT4C+on88Ypz3yzAKBggqhkjOPQQDBAOBiwAwgYcCQSLhEaJvML/nYseBhrkhOs/tFSw1nFgyi4n9g8i0K5kZDdRC9N7ZyckvwlL05F+MEgx7hGOjO4AeQ5cMCE8T182SAkIAqsRSj3LxGXLmht95L3q/p01It++L2lDZsSEZ1Buxzxq9sBVX1uMjWAItqxtKenMv97z0uRaPzKtQC5eVpSqEuNQ=";
	private static final String LEAF_PRIVATE_KEY = "MIHuAgEAMBAGByqGSM49AgEGBSuBBAAjBIHWMIHTAgEBBEIB79ZEPDL+/MuwX+bzp6uKu9iUiAH5ZnS3anRVl56Oq9Bjs4FhcTqSgwI5z/EuZx2Ht53y2eho7GH9zZOZOsOoIaWhgYkDgYYABAGw9DT7enuCtUKCsIPGx0Ff536f+zYs1JHespryZoR4yNHqWsXSnfZDWqjqFjoN82f9u7ah8DyCQf7sYSsdNZzaQwCFXQwg9ZlN2agUFe/KFe3rwxyl52fzMPBHsffbXVLNbnOZGs1jPWzbbnC+HcJwsfkcJmcJd/GyDhiTEXNpXPZJlw==";

	LicenseValidator validator = new LicenseValidator();

	@BeforeEach
	void setup() {
		var verifierProducer = new LicenseVerifierProducer();
		validator.verifier = verifierProducer.produceLicenseVerifier();
	}

	@Test
	@DisplayName("validate valid legacy token (pre org.cryptomator.hub.entitlements)")
		// TODO: eventually remove this test when legacy tokens are no longer supported, see https://github.com/cryptomator/hub/issues/391
	void testValidateValidToken() {
		var jwt = validator.validate(VALID_LEGACY_TOKEN, "42");
		Assertions.assertEquals("ES512", jwt.getAlgorithm());
		Assertions.assertEquals("42", jwt.getId());
		Assertions.assertEquals("Skymatic", jwt.getIssuer());
		Assertions.assertEquals("Cryptomator Hub", jwt.getAudience().get(0));
		Assertions.assertEquals("hub@cryptomator.org", jwt.getSubject());
		Assertions.assertEquals(5, jwt.getClaim("seats").asInt());
		Assertions.assertEquals(253402214400l, jwt.getExpiresAt().getTime() / 1000);
		Assertions.assertEquals("http://localhost:8787/hub/subscription?hub_id=42", jwt.getClaim("refreshUrl").asString());
	}

	@Test
	@DisplayName("validate valid token with mismatching hub id")
	void testValidateValidTokenWithMismatchingHubId() {
		Assertions.assertThrows(InvalidClaimException.class, () -> {
			validator.validate(VALID_LEGACY_TOKEN, "123");
		});
	}

	@Test
	@DisplayName("validate expired token")
	void testValidateExpiredToken() {
		// this should not throw an exception and return a JWT with an expired date
		validator.validate(EXPIRED_TOKEN, "42");
	}

	@Test
	@DisplayName("validate future token")
	void testValidateFutureToken() {
		// this should not throw an exception and return a JWT with an issued at in the future
		validator.validate(FUTURE_TOKEN, "42");
	}

	@Test
	@DisplayName("validate token with invalid signature")
	void testValidateTokenWithInvalidSignature() {
		Assertions.assertThrows(SignatureVerificationException.class, () -> {
			validator.validate(TOKEN_WITH_INVALID_SIGNATURE, "42");
		});
	}

	@Test
	@DisplayName("validate malformed token")
	void testValidateMalformedToken() {
		Assertions.assertThrows(JWTDecodeException.class, () -> {
			validator.validate(MALFORMED_TOKEN, "42");
		});
	}

	@Test
	@DisplayName("validate token with x5c certificate chain")
	void testValidateTokenWithX5cCertificateChain() {
		var verifierProducer = new LicenseVerifierProducer();
		validator.verifier = verifierProducer.produceLicenseVerifier(ROOT_CERTIFICATE);
		var token = JWT.create()
				.withHeader(Map.of("x5c", List.of(LEAF_CERTIFICATE, INTERMEDIATE_CERTIFICATE)))
				.withJWTId("42")
				.withSubject("hub@cryptomator.org")
				.withIssuedAt(new java.util.Date())
				.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 60_000))
				.withClaim("seats", 5)
				.sign(Algorithm.ECDSA512(null, decodePrivateKey(LEAF_PRIVATE_KEY)));
		var jwt = validator.validate(token, "42");
		Assertions.assertEquals("42", jwt.getId());
	}

	@Test
	@DisplayName("reject token with invalid x5c certificate chain")
	void testRejectTokenWithInvalidX5cCertificateChain() {
		var verifierProducer = new LicenseVerifierProducer();
		validator.verifier = verifierProducer.produceLicenseVerifier(ROOT_CERTIFICATE);
		var token = JWT.create()
				.withHeader(Map.of("x5c", List.of(LEAF_CERTIFICATE)))
				.withJWTId("42")
				.withSubject("hub@cryptomator.org")
				.withIssuedAt(new java.util.Date())
				.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 60_000))
				.withClaim("seats", 5)
				.sign(Algorithm.ECDSA512(null, decodePrivateKey(LEAF_PRIVATE_KEY)));
		Assertions.assertThrows(JWTVerificationException.class, () -> {
			validator.validate(token, "42");
		});
	}

	private static ECPrivateKey decodePrivateKey(String encodedPkcs8PrivateKey) {
		try {
			var keyBytes = Base64.getDecoder().decode(encodedPkcs8PrivateKey);
			return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("Cannot decode test private key", e);
		}
	}
}
