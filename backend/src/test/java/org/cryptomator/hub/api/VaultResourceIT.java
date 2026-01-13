package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.agroal.api.AgroalDataSource;
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
import jakarta.validation.Validator;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.rollback.DBRollbackAfter;
import org.cryptomator.hub.rollback.DBRollbackBefore;
import org.flywaydb.core.Flyway;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@QuarkusTest
@DisplayName("Resource /vaults")
public class VaultResourceIT {

	@Inject
	AgroalDataSource dataSource;
	@Inject
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;
	@Inject
	Vault.Repository vaultRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	User.Repository userRepo;
	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;
	@Inject
	Validator validator;
	@InjectMock
	LicenseHolder licenseHolder;

	@SuppressWarnings("unused") // used by @DBRollbackAfter annotation
	@Inject
	public Flyway flyway;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@BeforeEach
	@Transactional
	void setupTestData() {
		var user998 = new User();
		user998.setId("user998");
		user998.setName("User 998");

		var user999 = new User();
		user999.setId("user999");
		user999.setName("User 999");
		user999.setEcdhPublicKey("ecdh_public999");
		user999.setEcdsaPublicKey("ecdsa_public999");
		user999.setPrivateKeys("private999");
		user999.setSetupCode("setup999");

		userRepo.persist(user998, user999);

		var group2 = groupRepo.findById("group2");
		group2.getMembers().add(user998);
		group2.getMembers().add(user999);
		groupRepo.persist(group2);

		effectiveGroupMembershipRepo.updateUsers(List.of("user998", "user999"));
		effectiveGroupMembershipRepo.updateGroups(List.of("group2"));

		var entitlements = new HubLicenseEntitlements(5L, 0L, null, null);
		Mockito.doReturn(entitlements).when(licenseHolder).getEntitlements();
		Mockito.doReturn(false).when(licenseHolder).isExpired();
		Mockito.doReturn(5L).when(licenseHolder).getSeats();
	}

	@AfterEach
	@Transactional
	void cleanupTestData() {
		userRepo.deleteByIds(List.of("user998", "user999"));
	}

	@Nested
	@DisplayName("Test VaultDto validation")
	public class TestVaultDtoValidation {

		private static final UUID VALID_ID = UUID.fromString("7E57C0DE-0000-4000-8000-000100001111");
		private static final String VALID_NAME = "foobar";
		private static final String VALID_MASTERKEY = "base64";
		private static final String VALID_SALT = "base64";
		private static final String VALID_AUTH_PUB = "base64";
		private static final String VALID_AUTH_PRI = "base64";

		@Test
		void testValidDto() {
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", false, Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, 8, VALID_SALT, VALID_AUTH_PUB, VALID_AUTH_PRI);
			var violations = validator.validate(dto);
			MatcherAssert.assertThat(violations, Matchers.empty());
		}

	}

	@Nested
	@DisplayName("As user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsAuthorizedUser1 {

		@Test
		@DisplayName("GET /vaults/accessible returns 200")
		void testGetSharedOrOwnedNotArchived() {
			when().get("/vaults/accessible")
					.then().statusCode(200)
					.body("id", hasItems(equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100002222")));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111 returns 200")
		void testGetVault1() {
			when().get("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"));
		}

		@Test
		@DisplayName("GET /vaults/nonExistingVault returns 404")
		void testGetVault2() {
			when().get("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token returns 200 using user access")
		void testUnlock1() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/access-token returns 200 using group access")
		void testUnlock2() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault2.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token returns 200 using user access with evenIfArchived set")
		void testUnlock3() {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=true", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token with remote IP and device ID stores it in audit log")
		void testUnlock4() throws SQLException {
			given().header("HUB-DEVICE-ID", "123456789123456789")
					.header("X-Forwarded-For", "1.2.3.4")
					.when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				var rs = s.executeQuery("""
						SELECT * FROM "audit_event_vault_key_retrieve" WHERE "device_id" = '123456789123456789' AND "ip_address" = '1.2.3.4';
						""");
				Assertions.assertTrue(rs.next());
			}
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 410 for archived vaults")
		void testUnlockArchived1() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(410);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 410 for archived vaults with evenIfArchived set to false")
		void testUnlockArchived2() {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=false", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(410);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 200 for archived vaults with evenIfArchived set to true")
		void testUnlockArchived3() throws SQLException {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=true", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(200);
		}

		@Nested
		@DisplayName("legacy unlock")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public class LegacyUnlock {

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/legacyDevice1 returns 200 using user access")
			void testUnlock1() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "legacyDevice1")
						.then().statusCode(200)
						.body(is("legacy.jwe.jwe.vault1.device1"));
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/keys/legacyDevice3 returns 200 using group access")
			void testUnlock2() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
						.then().statusCode(200)
						.body(is("legacy.jwe.jwe.vault2.device3"));
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/noSuchDevice returns 403") // legacy unlock must not encourage to register a legacy device by responding with 404 here
			void testUnlock3() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "noSuchDevice")
						.then().statusCode(403);
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/legacyDevice2 returns 403")
			void testUnlock4() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "legacyDevice2")
						.then().statusCode(403);
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/keys/someDevice returns 410 for archived vaults")
			void testUnlockArchived() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-00010000AAAA", "legacyDevice1")
						.then().statusCode(410);
			}

		}

	}

	@Nested
	@DisplayName("As user2")
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	public class AsAuthorizedUser2 {

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token returns 449, because user2 is not initialized")
		@DBRollbackBefore
		void testUnlock() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(449);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 403 for missing role")
		void testCreateVaultWithMissingRole() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100003333")
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As vault admin user1")
	@TestSecurity(user = "User Name 1", roles = {"user", "create-vaults"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class CreateVaults {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 201")
		void testCreateVault1() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100003333")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100003333"))
					.body("name", equalTo("My Vault"))
					.body("description", equalTo("Test vault 3"))
					.body("archived", equalTo(false));
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD returns 400 due to malformed request body")
		void testCreateVault2() {
			given().contentType(ContentType.JSON)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD") // invalid body (expected json)
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100004444 returns 201 ignoring archived flag")
		void testCreateVault3() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100004444");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 4", true, Instant.parse("2112-12-21T21:12:21Z"), "masterkey4", 42, "NaCl", "authPubKey4", "authPrvKey4");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100004444")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100004444"))
					.body("name", equalTo("My Vault"))
					.body("description", equalTo("Test vault 4"))
					.body("archived", equalTo(false));
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 200, updating only name, description and archive flag")
		void testUpdateVault() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "VaultUpdated", "Vault updated.", true, Instant.parse("2222-11-11T11:11:11Z"), "doNotUpdate", 27, "doNotUpdate", "doNotUpdate", "doNotUpdate");
			given().contentType(ContentType.JSON)
					.body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100003333")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100003333"))
					.body("name", equalTo("VaultUpdated"))
					.body("description", equalTo("Vault updated."))
					.body("archived", equalTo(true))
					.body("creationTime", not("2222-11-11T11:11:11Z"));
		}

	}

	@Nested
	@DisplayName("As vault admin user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public class GrantAccess {

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 404 for [user1, user666]")
		void testGrantAccess0() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault1.user1", "user666", "jwe.jwe.jwe.vault1.user666"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 200 for [user998, user999]")
		void testGrantAccess1() {
			given().contentType(ContentType.JSON).body(Map.of("user998", "jwe.jwe.jwe.vault1.user998", "user999", "jwe.jwe.jwe.vault1.user999"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 200 for user1")
		void testGrantAccess2() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault1.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/access-tokens returns 403 (not owning this vault)")
		void testGrantAccess3() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault666.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 404 for nonExistingUser")
		void testGrantAccess4() {
			given().contentType(ContentType.JSON).body(Map.of("user666", "jwe.jwe.jwe.vault1.user666"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 400 for empty body")
		void testGrantAccess5() {
			given().contentType(ContentType.JSON).body(Map.of())
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-tokens returns 200 for user1 and vault archived")
		void testGrantAccessArchived() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vaultAAA.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(200);
		}

	}

	@Nested
	@DisplayName("Managing members as user2")
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageAccessAsUser2 {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/users/user9999 returns 404 - no such user")
		void addNonExistingUser() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user9999")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/users/user2 returns 403 - not owning a nonexisting vault")
		void addUserToNonExistingVault() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD", "user2")
					.then().statusCode(403);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does not contain user2")
		void getMembersOfVault2a() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members returns 403")
		void getMembersOfVault1() {
			when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(403);
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains user2 via group membership")
		void testGetUsersRequiringAccess1() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE "user_details"
						SET ecdh_publickey='public2', ecdsa_publickey='ecdsa_public2', privatekeys='private2', setupcode='setup2'
						WHERE id='user2';
						""");
			}

			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET ecdh_publickey=NULL, ecdsa_publickey=NULL, privatekeys=NULL, setupcode=NULL
						WHERE id='user2';
						""");
			}
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 201")
		void testGrantDirectAccessToSelf() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(7)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does contain user2 directly")
		void getMembersOfVault2b() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
		}

		@Test
		@Order(10)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains user2")
		void testGetUsersRequiringAccess2() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET ecdh_publickey='ecdh_public2', ecdsa_publickey='ecdsa_public2', privatekeys='private2', setupcode='setup2'
						WHERE id='user2';
						""");
			}

			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET ecdh_publickey=NULL, ecdsa_publickey=NULL, privatekeys=NULL, setupcode=NULL
						WHERE id='user2';
						""");
			}
		}

		@Test
		@Order(11)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100002222/access-tokens for user2 returns 200")
		void testGrantAccess() {
			given().contentType(ContentType.JSON).body(Map.of("user2", "jwe.jwe.jwe.vault2.user2"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains not user2")
		void testGetUsersRequiringAccess3() {
			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
		}

		@Test
		@Order(13)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 204")
		void testRevokeAccess() { // previously added in testGrantAccess()
			given().when().delete("/vaults/{vaultId}/authority/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(14)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does not contain user2")
		@DBRollbackAfter
		void getMembersOfVault2c() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
		}
	}

	@Nested
	@DisplayName("Managing groups as user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageAccessAsUser1 {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group3000 returns 404")
		void addNonExistingGroup() {
			given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group3000")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 201")
		void addGroupToVault() {
			given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does contain group2 with memberSize=3")
		void getMembersOfVault1a() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("find { it.id == 'group2' }.memberSize", equalTo(3));
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members returns 403")
		void getMembersOfVault2() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(403);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant contains user999")
		void testGetUsersRequiringAccess3() {
			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("user999"));
		}

		@Test
		@Order(5)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens for user999 returns 200")
		void testGrantAccess2() {
			given().contentType(ContentType.JSON).body(Map.of("user999", "jwe.jwe.jwe.vault2.user999"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);

			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("user999")));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 204")
		void removeGroup2() {
			given().when().delete("/vaults/{vaultId}/authority/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(204);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does not contain group2")
		@DBRollbackAfter
		void getMembersOfVault1b() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("group2")));
		}

	}

	@Nested
	@DisplayName("Claim Ownership")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ClaimOwnership {

		private static Algorithm JWT_ALG;

		@BeforeAll
		void setup() throws GeneralSecurityException {
			var keyPairGen = KeyPairGenerator.getInstance("EC");
			keyPairGen.initialize(new ECGenParameterSpec("secp384r1"));
			var keyPair = keyPairGen.generateKeyPair();
			JWT_ALG = Algorithm.ECDSA384((ECPrivateKey) keyPair.getPrivate());

			//transaction context required for persistence
			QuarkusTransaction.requiringNew()
					.timeout(10)
					.call(() -> {
						Vault v = new Vault();
						v.setId(UUID.fromString("7E57C0DE-0000-4000-8000-000100009999"));
						v.setName("ownership-test-vault");
						v.setCreationTime(Instant.now());
						v.setAuthenticationPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
						vaultRepo.persist(v);
						return 0;
					});
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT has wrong SUB")
		void testClaimOwnershipIncorrectJWT1() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("userBAD")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT missing NBF")
		void testClaimOwnershipIncorrectJWT2() {
			var proof = JWT.create()
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT not yet valid")
		void testClaimOwnershipIncorrectJWT3() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().plusSeconds(60))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT missing EXP")
		void testClaimOwnershipIncorrectJWT4() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT expired")
		void testClaimOwnershipIncorrectJWT5() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().minusSeconds(60))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT wrong vaultId")
		void testClaimOwnershipIncorrectJWT6() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "wrong")
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT wrong signature")
		void testClaimOwnershipIncorrectJWT7() throws GeneralSecurityException {
			var keyPairGen = KeyPairGenerator.getInstance("EC");
			keyPairGen.initialize(new ECGenParameterSpec("secp384r1"));
			var differentKey = keyPairGen.generateKeyPair();
			var alg = Algorithm.ECDSA384((ECPrivateKey) differentKey.getPrivate());

			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(alg);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/claim-ownership returns 404")
		void testClaimOwnershipNoSuchVault() {
			var proof = JWT.create()
					.withJWTId(UUID.randomUUID().toString())
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.withIssuedAt(Instant.now())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 200")
		void testClaimOwnershipSuccess() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(200);
		}

		@Test
		@Order(3)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 409")
		void testClaimOwnershipAlreadyClaimed() {
			var proof = JWT.create()
					.withNotBefore(Instant.now().minusSeconds(10))
					.withExpiresAt(Instant.now().plusSeconds(10))
					.withSubject("user1")
					.withClaim("vaultId", "7E57C0DE-0000-4000-8000-000100009999".toLowerCase())
					.sign(JWT_ALG);

			given().param("proof", proof)
					.when().post("/vaults/{vaultId}/claim-ownership", "7E57C0DE-0000-4000-8000-000100009999")
					.then().statusCode(409);
		}

		@AfterAll
		void cleanup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "vault" WHERE "id" = '7E57C0DE-0000-4000-8000-000100009999';
						""");
			}
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /vaults/accessible",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/members",
				"PUT, /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user1",
				"DELETE, /vaults/7E57C0DE-0000-4000-8000-000100001111/authority/user1",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

	@Nested
	@DisplayName("/vaults/all")
	public class GetAllVaults {

		@Test
		@DisplayName("GET /vaults/all returns 403 as user")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		void testGetAllVaultsAsUser() {
			when().get("/vaults/all")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("GET /vaults/all returns 200 as user")
		@TestSecurity(user = "User Name 1", roles = {"admin"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		void testGetAllVaultsAsAdmin() {
			when().get("/vaults/all")
					.then().statusCode(200)
					.body("id", hasItems(equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100002222"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-00010000AAAA")));
		}
	}

	@Nested
	@DisplayName("/vaults/some")
	public class GetSomeVaults {

		@Nested
		@DisplayName("as admin")
		@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public class AsAdmin {

			@Test
			@DisplayName("GET /vaults/some?ids=7e57c0de-0000-4000-8000-000100001111&ids=7e57c0de-0000-4000-8000-000100002222")
			void testListSomeVaults() {
				given().param("ids", "7e57c0de-0000-4000-8000-000100001111", "7e57c0de-0000-4000-8000-000100002222")
						.when().get("/vaults/some")
						.then().statusCode(200)
						.body("id", Matchers.containsInAnyOrder(comparesEqualTo("7e57c0de-0000-4000-8000-000100001111"), comparesEqualTo("7e57c0de-0000-4000-8000-000100002222")));
			}

			@Test
			@DisplayName("GET /vaults/some?ids=7e57c0de-0000-4000-8000-BADBADBADBAD")
			void testListSomeVaultsNotExistingId() {
				given().param("ids", "7e57c0de-0000-4000-8000-BADBADBADBAD")
						.when().get("/vaults/some")
						.then().statusCode(200)
						.body("", hasSize(0));
			}

			@Test
			@DisplayName("GET /vaults/some")
			void testListSomeVaultsNoParams() {
				given().when().get("/vaults/some")
						.then().statusCode(200)
						.body("", hasSize(0));
			}
		}

		@Test
		@DisplayName("GET /vaults/some?ids=7e57c0de-0000-4000-8000-000100001111 returns 403 as user")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		void testListSomeVaultsAsUser() {
			given().param("ids", "7e57c0de-0000-4000-8000-000100001111")
					.when().get("/vaults/some")
					.then().statusCode(403);
		}
	}
}
