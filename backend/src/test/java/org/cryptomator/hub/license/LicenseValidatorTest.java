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
	private static final String EXPIRED_TOKEN = "eyJ4NWMiOlsiTUlJQ0REQ0NBYjZnQXdJQkFnSVVETU96eEpZMzgxSmpkTjgyY1c3cWQrQjN2aDB3QlFZREsyVndNRTR4Q3pBSkJnTlZCQVlUQWtSRk1SWXdGQVlEVlFRS0RBMVRhM2x0WVhScFl5QkhiV0pJTVNjd0pRWURWUVFEREI1TWFXTmxibk5sSUVsdWRHVnliV1ZrYVdGMFpTQkRRU0FvVkdWemRDa3dIaGNOTWpZd01qSTJNVFUwTURNMldoY05Nell3TWpJME1UVTBNRE0yV2pCSU1Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERWhNQjhHQTFVRUF3d1lUR2xqWlc1elpTQkpjM04xWlhJZ1EwRWdLRlJsYzNRcE1JR2JNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQWpBNEdHQUFRQmlVVlEwSGhaTXVBT3FpTzJsUElUK01NU0g0YmNsNkJPV25GbjIwNWJ6VGNSSTlSdVJkdHJYVk53cFwvSVB0ak1WWFRqXC9vVzByMTJIY3JFZExtaTlRSTZRQVNURUJ5V0xOVFNcL2Q5NElvWG1SWVFUbkMrUnRIK0hcLzRJMVRXWXc5MGFpaWcyeVYwRzFzMHFDZ0FpeUtzd2orU1Q2cjcxTk1cL2dlcG1sVzMrcWl2OVwvUFdqUWpCQU1CMEdBMVVkRGdRV0JCU05qQnd2K1wvaVlRdnBPT3F6MDJ1N3hhQVNTSVRBZkJnTlZIU01FR0RBV2dCUk1BU3NwMWtpYXdKbThZb0o2KzhcL3NxMjFYNHpBRkJnTXJaWEFEUVFBNE9rXC8reTBiZHptMlJVbWtIZDZRRlM2V2JCS2Y5TzR6ejNVYzdpQk1wS0lxMWtCbHErN1RiYmdNSEp1K2FZYk9EY1JXVCsrNXN4NGkyT3Nwa2dPc0oiLCJNSUlCdFRDQ0FXZWdBd0lCQWdJVUpteDhVcXRnbktjYXVRYnFiMDRUYVVCRytVSXdCUVlESzJWd01EOHhDekFKQmdOVkJBWVRBa1JGTVJZd0ZBWURWUVFLREExVGEzbHRZWFJwWXlCSGJXSklNUmd3RmdZRFZRUUREQTlNYVdObGJuTmxJRlJsYzNRZ1EwRXdIaGNOTWpZd01qSTJNVFV6TmpNMldoY05Nell3TWpJME1UVXpOak0yV2pCT01Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERW5NQ1VHQTFVRUF3d2VUR2xqWlc1elpTQkpiblJsY20xbFpHbGhkR1VnUTBFZ0tGUmxjM1FwTUNvd0JRWURLMlZ3QXlFQUczdnI4Tks3WVpzMXE2cFF0SmhIRGJUMnhNRDNDMnlzbXNuZW13MUZRbGFqWmpCa01CSUdBMVVkRXdFQlwvd1FJTUFZQkFmOENBUUF3RGdZRFZSMFBBUUhcL0JBUURBZ0VHTUIwR0ExVWREZ1FXQkJSTUFTc3Axa2lhd0ptOFlvSjYrOFwvc3EyMVg0ekFmQmdOVkhTTUVHREFXZ0JRQmlPK3lxTVRhZzU5ZUg4ZmdjRjlnTXNYZUV6QUZCZ01yWlhBRFFRQktkR0cybmpWa3JqMEwxbWVXVTROcGxaZHlHUTJxYUNxNFBSenk1OUg5WW1EUzc5M1JsTWE0TU9ad0FtVGtVUTV2YWt1dnR6MTM0SWl6NmpKa0RCZ0YiXSwiYWxnIjoiRVM1MTIifQ.eyJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAZXhhbXBsZS5jb20iLCJyZWZyZXNoVXJsIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcL1wvaHViXC9yZWZyZXNoIiwia2lkIjoiWUVWMUVJdjl2WUxHWV9HVGhDRU9Ma2V5bXFQMGVlWFVVN01tbEpocWdHQSIsIm9yZy5jcnlwdG9tYXRvci5odWIuZW50aXRsZW1lbnRzIjp7InNlYXRzIjo1LCJzaG93VHJpYWxIaW50IjpmYWxzZSwiYXVkaXRMb2dSZXRlbnRpb25EYXlzIjo3LCJlbWVyZ2VuY3lBY2Nlc3NFbmFibGVkIjpmYWxzZSwia2V5Y2xvYWtBY2Nlc3NFbmFibGVkIjp0cnVlfSwiaXNzIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcLyIsImV4cCI6OTQ2Njg0ODAwLCJpYXQiOjE3NzIxMjE1ODIsInNlYXRzIjo1LCJqdGkiOiI0MiJ9.AGYclI1IqLqBhh0_giYH3B83Ffmga5_Lr7YuE1S_upyBXedI70cYJTyMAWQcakFdIKF9k9exWHjldCdnF3QoyhHfANgmnnb4bAkSlX59WFXePMbks97SvVvMXExaCIvKAQWbGJ0jNw4aid-6plrNYP855jATfEZh800Fcsbp81eChysO";
	private static final String FUTURE_TOKEN = "eyJ4NWMiOlsiTUlJQ0REQ0NBYjZnQXdJQkFnSVVETU96eEpZMzgxSmpkTjgyY1c3cWQrQjN2aDB3QlFZREsyVndNRTR4Q3pBSkJnTlZCQVlUQWtSRk1SWXdGQVlEVlFRS0RBMVRhM2x0WVhScFl5QkhiV0pJTVNjd0pRWURWUVFEREI1TWFXTmxibk5sSUVsdWRHVnliV1ZrYVdGMFpTQkRRU0FvVkdWemRDa3dIaGNOTWpZd01qSTJNVFUwTURNMldoY05Nell3TWpJME1UVTBNRE0yV2pCSU1Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERWhNQjhHQTFVRUF3d1lUR2xqWlc1elpTQkpjM04xWlhJZ1EwRWdLRlJsYzNRcE1JR2JNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQWpBNEdHQUFRQmlVVlEwSGhaTXVBT3FpTzJsUElUK01NU0g0YmNsNkJPV25GbjIwNWJ6VGNSSTlSdVJkdHJYVk53cFwvSVB0ak1WWFRqXC9vVzByMTJIY3JFZExtaTlRSTZRQVNURUJ5V0xOVFNcL2Q5NElvWG1SWVFUbkMrUnRIK0hcLzRJMVRXWXc5MGFpaWcyeVYwRzFzMHFDZ0FpeUtzd2orU1Q2cjcxTk1cL2dlcG1sVzMrcWl2OVwvUFdqUWpCQU1CMEdBMVVkRGdRV0JCU05qQnd2K1wvaVlRdnBPT3F6MDJ1N3hhQVNTSVRBZkJnTlZIU01FR0RBV2dCUk1BU3NwMWtpYXdKbThZb0o2KzhcL3NxMjFYNHpBRkJnTXJaWEFEUVFBNE9rXC8reTBiZHptMlJVbWtIZDZRRlM2V2JCS2Y5TzR6ejNVYzdpQk1wS0lxMWtCbHErN1RiYmdNSEp1K2FZYk9EY1JXVCsrNXN4NGkyT3Nwa2dPc0oiLCJNSUlCdFRDQ0FXZWdBd0lCQWdJVUpteDhVcXRnbktjYXVRYnFiMDRUYVVCRytVSXdCUVlESzJWd01EOHhDekFKQmdOVkJBWVRBa1JGTVJZd0ZBWURWUVFLREExVGEzbHRZWFJwWXlCSGJXSklNUmd3RmdZRFZRUUREQTlNYVdObGJuTmxJRlJsYzNRZ1EwRXdIaGNOTWpZd01qSTJNVFV6TmpNMldoY05Nell3TWpJME1UVXpOak0yV2pCT01Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERW5NQ1VHQTFVRUF3d2VUR2xqWlc1elpTQkpiblJsY20xbFpHbGhkR1VnUTBFZ0tGUmxjM1FwTUNvd0JRWURLMlZ3QXlFQUczdnI4Tks3WVpzMXE2cFF0SmhIRGJUMnhNRDNDMnlzbXNuZW13MUZRbGFqWmpCa01CSUdBMVVkRXdFQlwvd1FJTUFZQkFmOENBUUF3RGdZRFZSMFBBUUhcL0JBUURBZ0VHTUIwR0ExVWREZ1FXQkJSTUFTc3Axa2lhd0ptOFlvSjYrOFwvc3EyMVg0ekFmQmdOVkhTTUVHREFXZ0JRQmlPK3lxTVRhZzU5ZUg4ZmdjRjlnTXNYZUV6QUZCZ01yWlhBRFFRQktkR0cybmpWa3JqMEwxbWVXVTROcGxaZHlHUTJxYUNxNFBSenk1OUg5WW1EUzc5M1JsTWE0TU9ad0FtVGtVUTV2YWt1dnR6MTM0SWl6NmpKa0RCZ0YiXSwiYWxnIjoiRVM1MTIifQ.eyJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAZXhhbXBsZS5jb20iLCJyZWZyZXNoVXJsIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcL1wvaHViXC9yZWZyZXNoIiwia2lkIjoiWUVWMUVJdjl2WUxHWV9HVGhDRU9Ma2V5bXFQMGVlWFVVN01tbEpocWdHQSIsIm9yZy5jcnlwdG9tYXRvci5odWIuZW50aXRsZW1lbnRzIjp7InNlYXRzIjo1LCJzaG93VHJpYWxIaW50IjpmYWxzZSwiYXVkaXRMb2dSZXRlbnRpb25EYXlzIjo3LCJlbWVyZ2VuY3lBY2Nlc3NFbmFibGVkIjpmYWxzZSwia2V5Y2xvYWtBY2Nlc3NFbmFibGVkIjp0cnVlfSwiaXNzIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcLyIsImV4cCI6MTc3MjEyMTg5OCwiaWF0IjozMjUwMzY4MDAwMCwic2VhdHMiOjUsImp0aSI6IjQyIn0.AZB8nxX0_x319_6ZIzLV2nol18k5BzBtujiF5BCqUfXkBzaKbNzhGGmVOvr1w_0LS2Yb8sJjlgDRw_bO3fIiAX8jAaWdo_M4yXFLxJrejvB1mi1oOujE1kecTXPuayrkLAOi99VYdcXqJB7pptqJcXC374QorZRQKFK-PnZ6YT6hNv5Z";
	private static final String TOKEN_WITH_INVALID_SIGNATURE = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.AbVUinMiT3J_03je8WTOIl-VdggzvoFgnOsdouAs-DLOtQzau9valrq-S6pETyi9Q18HH-EuwX49Q7m3KC0GuNBJAc9Tksulgsdq8GqwIqZqDKmG7hNmDzaQG1Dpdezn2qzv-otf3ZZe-qNOXUMRImGekfQFIuH_MjD2e8RZyww6lbZk";
	private static final String MALFORMED_TOKEN = "hello world";
	private static final String ROOT_CERTIFICATE = "MIIBpjCCAVigAwIBAgIUJWMTr10U1lkxgPhxjed4kPqSu4EwBQYDK2VwMD8xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNlbnNlIFRlc3QgQ0EwHhcNMjYwMjI2MTUzMzIwWhcNMzYwMjI0MTUzMzIwWjA/MQswCQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEYMBYGA1UEAwwPTGljZW5zZSBUZXN0IENBMCowBQYDK2VwAyEAVi5WsfyHgHiL0vr4d2Lt1g7kPgC5m8u0DIKLalKJqHSjZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQEwDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBQBiO+yqMTag59eH8fgcF9gMsXeEzAfBgNVHSMEGDAWgBQBiO+yqMTag59eH8fgcF9gMsXeEzAFBgMrZXADQQD1I6bFKdHW+MaSpNVl/seCJny0Tp5L3+v6IyybvV0e66ks8BhsRWbqoSSBEaF4zlX7gAPzNuOIEFadM4s1fVMC";
	private static final String INTERMEDIATE_CERTIFICATE = "MIIBtTCCAWegAwIBAgIUJmx8UqtgnKcauQbqb04TaUBG+UIwBQYDK2VwMD8xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNlbnNlIFRlc3QgQ0EwHhcNMjYwMjI2MTUzNjM2WhcNMzYwMjI0MTUzNjM2WjBOMQswCQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEnMCUGA1UEAwweTGljZW5zZSBJbnRlcm1lZGlhdGUgQ0EgKFRlc3QpMCowBQYDK2VwAyEAG3vr8NK7YZs1q6pQtJhHDbT2xMD3C2ysmsnemw1FQlajZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBRMASsp1kiawJm8YoJ6+8/sq21X4zAfBgNVHSMEGDAWgBQBiO+yqMTag59eH8fgcF9gMsXeEzAFBgMrZXADQQBKdGG2njVkrj0L1meWU4NplZdyGQ2qaCq4PRzy59H9YmDS793RlMa4MOZwAmTkUQ5vakuvtz134Iiz6jJkDBgF";
	private static final String INTERMEDIATE_CN = "License Intermediate CA (Test)";
	private static final String LEAF_CERTIFICATE = "MIICDDCCAb6gAwIBAgIUDMOzxJY381JjdN82cW7qd+B3vh0wBQYDK2VwME4xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMScwJQYDVQQDDB5MaWNlbnNlIEludGVybWVkaWF0ZSBDQSAoVGVzdCkwHhcNMjYwMjI2MTU0MDM2WhcNMzYwMjI0MTU0MDM2WjBIMQswCQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEhMB8GA1UEAwwYTGljZW5zZSBJc3N1ZXIgQ0EgKFRlc3QpMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBiUVQ0HhZMuAOqiO2lPIT+MMSH4bcl6BOWnFn205bzTcRI9RuRdtrXVNwp/IPtjMVXTj/oW0r12HcrEdLmi9QI6QASTEByWLNTS/d94IoXmRYQTnC+RtH+H/4I1TWYw90aiig2yV0G1s0qCgAiyKswj+ST6r71NM/gepmlW3+qiv9/PWjQjBAMB0GA1UdDgQWBBSNjBwv+/iYQvpOOqz02u7xaASSITAfBgNVHSMEGDAWgBRMASsp1kiawJm8YoJ6+8/sq21X4zAFBgMrZXADQQA4Ok/+y0bdzm2RUmkHd6QFS6WbBKf9O4zz3Uc7iBMpKIq1kBlq+7TbbgMHJu+aYbODcRWT++5sx4i2OspkgOsJ";
	private static final String LEAF_PRIVATE_KEY = "MGACAQAwEAYHKoZIzj0CAQYFK4EEACMESTBHAgEBBEIA+tBtqmK6OyXS+0ATPadXIF3mf1uwAY/ujIbhtox+dcqolusy8fR8cIVYNqbRb8wUZvbY++xn24nsDAiw6Za4NTg=";

	LicenseValidator validator = new LicenseValidator();

	@BeforeEach
	void setup() {
		var verifierProducer = new LicenseVerifierProducer();
		verifierProducer.licenseChainRequiredCn = "";
		validator.verifier = verifierProducer.produceLicenseVerifier(ROOT_CERTIFICATE, INTERMEDIATE_CN);
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
		validator.verifier = verifierProducer.produceLicenseVerifier(ROOT_CERTIFICATE, INTERMEDIATE_CN);
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

	@Test
	@DisplayName("validate token with matching intermediate cert cn")
	void testValidateTokenWithMatchingIntermediateCn() {
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
	@DisplayName("reject token with mismatching intermediate cert cn")
	void testRejectTokenWithMismatchingIntermediateCn() {
		var verifierProducer = new LicenseVerifierProducer();
		validator.verifier = verifierProducer.produceLicenseVerifier(ROOT_CERTIFICATE, "some other CN");
		var token = JWT.create()
				.withHeader(Map.of("x5c", List.of(LEAF_CERTIFICATE, INTERMEDIATE_CERTIFICATE)))
				.withJWTId("42")
				.withSubject("hub@cryptomator.org")
				.withIssuedAt(new java.util.Date())
				.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 60_000))
				.withClaim("seats", 5)
				.sign(Algorithm.ECDSA512(null, decodePrivateKey(LEAF_PRIVATE_KEY)));
		Assertions.assertThrows(JWTVerificationException.class, () -> validator.validate(token, "42"));
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
