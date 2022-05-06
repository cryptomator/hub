package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LicenseValidatorTest {

	private static final String VALID_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm";
	private static final String EXPIRED_TOKEN = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA1MTA0MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6LTYyMTY3MjE5MjAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AK9Du5MNsyVgOzicgi2S7ECxxqZPBLP8cFByAAZ7_y96NEvrwOiR8NNmlZlfvebMfckaYUEg-nf3BAd1JHAxur1UADyYbKSLoMs4B69SkbKW0drjfY9RjFUhO_w6sS4gg39_X_IhbwW6wRkGFoqGRI0juaCPViQqV5WFIcj7RbuCcNJ6";
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
		Assertions.assertThrows(TokenExpiredException.class, () -> {
			validator.validate(EXPIRED_TOKEN, "42");
		});
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

}
