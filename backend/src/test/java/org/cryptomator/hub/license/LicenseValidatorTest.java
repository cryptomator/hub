package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class LicenseValidatorTest {

	private static final String VALID_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm";
	private static final String EXPIRED_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY3NzA4MzI1OSwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6OTQ2Njg0ODAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.APQnWig9ZyT6_xRviPVs3YPTaP1w_YXTpWULgvsUpCGmGQwEmT6nl0x2jNB_jkQi93E7tr9WvipvX5DkXUOYJP3OAJjzPdN7rTX2tnXTKO8irshkcqmvt79v1E4k50YLkwP-1NIwiO_ltp5sezhLbzOVPXRag6mQfc0KvS6PiZTYGYQh";
	private static final String FUTURE_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOjQyLCJpYXQiOjE3MDEyNDkzMzEzMSwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJ0b2JpYXMuaGFnZW1hbm5Ac2t5bWF0aWMuZGUiLCJzZWF0cyI6NSwiZXhwIjoxNzIyMzg0MDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.ALd0oyPR3kgntysXp8TZ1LvmHYDiDIGlbmaq52d5wAE1V8MZ1asWvufXgL9YExXvJhFbGCnLu66XgA387rxjrxKeASL_q43ZZUEDxtm8aa7uH2VMOvdM3gXEibSHUzNwO0MRWFbeYWOc8daRNWdxgOcrpX6NcMV7vPZH7yZSEct_cqf5";
	private static final String TOKEN_WITH_INVALID_SIGNATURE = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.AbVUinMiT3J_03je8WTOIl-VdggzvoFgnOsdouAs-DLOtQzau9valrq-S6pETyi9Q18HH-EuwX49Q7m3KC0GuNBJAc9Tksulgsdq8GqwIqZqDKmG7hNmDzaQG1Dpdezn2qzv-otf3ZZe-qNOXUMRImGekfQFIuH_MjD2e8RZyww6lbZk";
	private static final String MALFORMED_TOKEN = "hello world";

	LicenseValidator validator = new LicenseValidator();

	@Test
	@DisplayName("validate valid token")
	public void testValidateValidToken() {
		var jwt = validator.validate(VALID_TOKEN, "42");
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
	public void testValidateValidTokenWithMismatchingHubId() {
		Assertions.assertThrows(InvalidClaimException.class, () -> {
			validator.validate(VALID_TOKEN, "123");
		});
	}

	@Test
	@DisplayName("validate expired token")
	public void testValidateExpiredToken() {
		// this should not throw an exception and return a JWT with an expired date
		validator.validate(EXPIRED_TOKEN, "42");
	}

	@Test
	@DisplayName("validate future token")
	public void testValidateFutureToken() {
		// this should not throw an exception and return a JWT with an issued at in the future
		validator.validate(FUTURE_TOKEN, "42");
	}

	@Test
	@DisplayName("validate token with invalid signature")
	public void testValidateTokenWithInvalidSignature() {
		Assertions.assertThrows(SignatureVerificationException.class, () -> {
			validator.validate(TOKEN_WITH_INVALID_SIGNATURE, "42");
		});
	}

	@Test
	@DisplayName("validate malformed token")
	public void testValidateMalformedToken() {
		Assertions.assertThrows(JWTDecodeException.class, () -> {
			validator.validate(MALFORMED_TOKEN, "42");
		});
	}

	@Test
	@DisplayName("validate token's refreshURL")
	public void testGetTokensRefreshUrl() {
		Assertions.assertEquals(Optional.of("http://localhost:8787/hub/subscription?hub_id=42"), validator.refreshUrl(VALID_TOKEN));
	}

	@Test
	@DisplayName("validate expired token's refreshURL")
	public void testGetExpiredTokensRefreshUrl() {
		// this should not throw an exception and return a JWT with an expired date
		Assertions.assertEquals(Optional.of("http://localhost:8787/hub/subscription?hub_id=42"), validator.refreshUrl(EXPIRED_TOKEN));
	}

	@Test
	@DisplayName("validate expired token's refreshURL with invalid signature")
	public void testInvalidSignatureTokensRefreshUrl() {
		Assertions.assertThrows(SignatureVerificationException.class, () -> {
			validator.refreshUrl(TOKEN_WITH_INVALID_SIGNATURE);
		});
	}

}
