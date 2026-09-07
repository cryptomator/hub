package org.cryptomator.hub.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.EmergencyRecoveryProcess;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.rollback.DBRollbackAfter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@DisplayName("Resource /emergency-access")
public class EmergencyAccessResourceIT {

	// user1 is a member of this vault's emergency access council (see V9999__Test_Data.sql); user2 is not.
	private static final UUID COUNCIL_VAULT_ID = UUID.fromString("7E57C0DE-0000-4000-8000-000100001111");
	// user1 is NOT a member of this vault's emergency access council.
	private static final UUID OTHER_VAULT_ID = UUID.fromString("7E57C0DE-0000-4000-8000-000100002222");
	// existing recovery process for COUNCIL_VAULT started by user1:
	private static final UUID STARTED_RECOVERY_PROCESS_ID = UUID.fromString("7E57C0DE-0000-4000-8000-000200000001");

	@InjectMock
	EventLogger eventLogger; // mocked so starting a process does not persist audit events

	@Inject
	EmergencyRecoveryProcess.Repository recoveryRepo;

	@Inject
	@SuppressWarnings("unused") // needed for @DBRollbackAfter
	public Flyway flyway;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	// Builds a valid RecoveryProcessDto payload in which {@code memberId} is the sole council member of the process.
	private static String recoveryProcessBody(UUID processId, UUID vaultId, String memberId) {
		return """
				{
					"id": "%s",
					"vaultId": "%s",
					"type": "CHANGE_PERMISSIONS",
					"requiredKeyShares": 2,
					"processPublicKey": "processPublicKey",
					"recoveredKeyShares": {
						"%s": {
							"processPrivateKey": "jwe.jwe.jwe.process.privatekey",
							"unrecoveredKeyShare": "jwe.jwe.jwe.unrecovered.share",
							"recoveredKeyShare": "jwe.jwe.jwe.recovered.share",
							"signedProcessInfo": "jws.jws.signature"
						}
					}
				}
				""".formatted(processId, vaultId, memberId);
	}

	private boolean processExists(UUID processId) {
		return QuarkusTransaction.requiringNew().call(() -> recoveryRepo.findByIdOptional(processId).isPresent());
	}

	@Nested
	@DisplayName("As non-council user2")
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	class AsNonCouncilUser2 {

		@Test
		@DisplayName("PUT /emergency-access/{processId} self-enrolling into the council returns 403")
		void testSelfEnrollIsForbidden() {
			var processId = UUID.fromString("7E57C0DE-0000-4000-8000-000200000002");

			// attacker submits a process listing their own (non-council) user id as recovery participant
			given().contentType(ContentType.JSON).body(recoveryProcessBody(processId, COUNCIL_VAULT_ID, "user2"))
					.when().put("/emergency-access/{processId}", processId)
					.then().statusCode(403);

			// the process must not have been persisted, otherwise the attacker would gain bypassForEmergencyAccess
			assertThat(processExists(processId), is(false));
		}

		@Test
		@DisplayName("GET /emergency-access/{vaultId} returns 403")
		void testFindByVaultIdIsForbidden() {
			given().when().get("/emergency-access/{vaultId}", COUNCIL_VAULT_ID)
					.then().statusCode(403);
		}

		@Test
		@DisplayName("DELETE /emergency-access/{processId}/abort returns 403")
		void testAbortIsForbidden() {
			given().when().delete("/emergency-access/{processId}/abort", STARTED_RECOVERY_PROCESS_ID)
					.then().statusCode(403);
		}

		@Test
		@DisplayName("DELETE /emergency-access/{processId}/complete returns 403")
		void testCompleteIsForbidden() {
			given().when().delete("/emergency-access/{processId}/complete", STARTED_RECOVERY_PROCESS_ID)
					.then().statusCode(403);
		}
	}

	@Nested
	@DisplayName("As council member user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	class AsCouncilMemberUser1 {

		@Test
		@DisplayName("PUT /emergency-access/{processId} for a vault the user is council of returns 204")
		@DBRollbackAfter
		void testCouncilMemberCanStartRecovery() {
			var processId = UUID.fromString("7E57C0DE-0000-4000-8000-000200000003");

			given().contentType(ContentType.JSON).body(recoveryProcessBody(processId, COUNCIL_VAULT_ID, "user1"))
					.when().put("/emergency-access/{processId}", processId)
					.then().statusCode(204);

			assertThat(processExists(processId), is(true));
		}

		@Test
		@DisplayName("PUT /emergency-access/{processId} for a vault the user is NOT council of returns 403")
		void testCouncilMembershipIsVaultSpecific() {
			var processId = UUID.fromString("7E57C0DE-0000-4000-8000-000200000003");

			// user1 is council of COUNCIL_VAULT_ID but not of OTHER_VAULT_ID
			given().contentType(ContentType.JSON).body(recoveryProcessBody(processId, OTHER_VAULT_ID, "user1"))
					.when().put("/emergency-access/{processId}", processId)
					.then().statusCode(403);

			assertThat(processExists(processId), is(false));
		}

		@Test
		@DisplayName("GET /emergency-access/{vaultId} returns 200")
		void testFindByVaultId() {
			given().when().get("/emergency-access/{vaultId}", COUNCIL_VAULT_ID)
					.then().statusCode(200);
		}

		@Test
		@DisplayName("DELETE /emergency-access/{processId}/abort returns 204")
		@DBRollbackAfter
		void testAbort() {
			given().when().delete("/emergency-access/{processId}/abort", STARTED_RECOVERY_PROCESS_ID)
					.then().statusCode(204);
		}

		@Test
		@DisplayName("DELETE /emergency-access/{processId}/complete returns 204")
		@DBRollbackAfter
		void testComplete() {
			given().when().delete("/emergency-access/{processId}/complete", STARTED_RECOVERY_PROCESS_ID)
					.then().statusCode(204);
		}

	}
}
