package org.cryptomator.hub.api;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class TestLicenses {

	/**
	 * Valid license. Used in V9999_Test_Data.sql
	 * Header:
	 * {
	 *   "alg": "ES512"
	 * }
	 * Payload:
	 * {
	 *   "jti": "42",
	 *   "iat": 1648049360,
	 *   "iss": "Skymatic",
	 *   "aud": "Cryptomator Hub",
	 *   "sub": "hub@cryptomator.org",
	 *   "seats": 5,
	 *   "exp": 253402214400,
	 *   "refreshUrl": "http://localhost:8787/hub/subscription?hub_id=42"
	 * }
	 */
	public static final String VALID = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm";

	/**
	 * Exceeded license. Number of total seats is 0.
	 * Header:
	 * {
	 *   "alg": "ES512"
	 * }
	 * Payload:
	 * {
	 *   "jti": "42",
	 *   "iat": 1709808354,
	 *   "iss": "Skymatic",
	 *   "aud": "Cryptomator Hub",
	 *   "sub": "hub@cryptomator.org",
	 *   "seats": 0,
	 *   "exp": 2524608000,
	 *   "refreshUrl": "https://cryptomator-store-dev.skymatic.workers.dev/api/hub/token"
	 * }
	 */
	public static final String EXCEEDED = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTcwOTgwODM1NCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjAsImV4cCI6MjUyNDYwODAwMCwicmVmcmVzaFVybCI6Imh0dHBzOi8vY3J5cHRvbWF0b3Itc3RvcmUtZGV2LnNreW1hdGljLndvcmtlcnMuZGV2L2FwaS9odWIvdG9rZW4ifQ.AafP9HbnSqO-N7dtfFExyAhAuRPjB1_Dd6NPtBgDhTx_E1dg4Mzi8y0ZGPaYluSwWbkCVk5SM3cubOuOctoIlGZ8AZWRn-GbUSGAXCy7oGlYv-GcQM9l5zLrJUvb0oBtU67WHWJCc4FytUn_YSijJ6qx-tfZX2PXQvyUnnaoRjq_SGCR";

	/**
	 * Expired license jwt. Expired at 1970-01-01T00:00:05Z.
	 * Header:
	 * {
	 *   "alg": "ES512"
	 * }
	 * Payload:
	 * {
	 *   "jti": "42",
	 *   "iat": 1648049360,
	 *   "iss": "Skymatic",
	 *   "aud": "Cryptomator Hub",
	 *   "sub": "hub@cryptomator.org",
	 *   "seats": 5,
	 *   "exp": 5,
	 *   "refreshUrl": "http://localhost:8787/hub/subscription?hub_id=42"
	 * }
	 */
	public static final String EXPIRED = "eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6NSwicmVmcmVzaFVybCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODc4Ny9odWIvc3Vic2NyaXB0aW9uP2h1Yl9pZD00MiJ9.Ac9p6Pvm1XXA06qL4tNKPH8ZodA6NnIWV7468LHYubvu7Bru0RhP-qA06mpNJJpf7jc_ElC76NHpdW1kR74KI66IACFhGNNx0rY3FibWzuOxCX7TSOaVd7NuxVTVusTflNmohmnG0pOVr5vvTYD-ynlvRE1WurRY6EBZqVz4VoOb-h3A";


	/**
	 * Invalid JWT
	 */
	public static final String MUMBOJUMBO = "not.a.valid.jwt";


	private TestLicenses() {
	}

	static void rollbackLicense(DataSource db) throws SQLException {
		changeLicense(db, TestLicenses.VALID);
	}

	static void exceedLicense(DataSource db) throws SQLException {
		changeLicense(db, TestLicenses.EXCEEDED);
	}

	static void expireLicense(DataSource db) throws SQLException {
		changeLicense(db, TestLicenses.EXPIRED);
	}

	private static void changeLicense(DataSource db, String newLicense) throws SQLException {
		try (var c = db.getConnection(); var s = c.createStatement()) {
			s.execute("""
					UPDATE "settings"
					SET hub_id = '42', license_key  = 'REPLACE_ME'
					WHERE id = 0;"""
					.replace("REPLACE_ME", newLicense)
			);
		}
	}
}
