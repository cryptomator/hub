package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Vault;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@QuarkusTest
@DisplayName("Resource /vaults")
public class VaultResourceTest {

	@Inject
	AgroalDataSource dataSource;

	@Inject
	Validator validator;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	private static PrivateKey getPrivateKey(String keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBytes)));
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
		private static final String VALID_METADATA = "base64";

		@Test
		public void testValidDto() {
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", false, Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, 8, VALID_SALT, VALID_AUTH_PUB
					, VALID_AUTH_PRI, VALID_METADATA
			);
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
		public void testGetSharedOrOwnedNotArchived() {
			when().get("/vaults/accessible")
					.then().statusCode(200)
					.body("id", hasItems(equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100002222")));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111 returns 200")
		public void testGetVault1() {
			when().get("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"));
		}

		@Test
		@DisplayName("GET /vaults/nonExistingVault returns 404")
		public void testGetVault2() {
			when().get("/vaults/{vaultId}", "nonExistingVault")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token returns 200 using user access")
		public void testUnlock1() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/access-token returns 200 using group access")
		public void testUnlock2() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault2.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-token returns 200 using user access with evenIfArchived set")
		public void testUnlock3() {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=true", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 410 for archived vaults")
		public void testUnlockArchived1() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(410);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 410 for archived vaults with evenIfArchived set to false")
		public void testUnlockArchived2() {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=false", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(410);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-token returns 403 for archived vaults with evenIfArchived set to true")
		public void testUnlockArchived3() throws SQLException {
			when().get("/vaults/{vaultId}/access-token?evenIfArchived=true", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(403);
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
			public void testUnlock1() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "legacyDevice1")
						.then().statusCode(200)
						.body(is("legacy.jwe.jwe.vault1.device1"));
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/keys/legacyDevice3 returns 200 using group access")
			public void testUnlock2() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
						.then().statusCode(200)
						.body(is("legacy.jwe.jwe.vault2.device3"));
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/noSuchDevice returns 403")
			// legacy unlock must not encourage to register a legacy device by responding with 404 here
			public void testUnlock3() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "noSuchDevice")
						.then().statusCode(403);
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/legacyDevice2 returns 403")
			public void testUnlock4() {
				when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "legacyDevice2")
						.then().statusCode(403);
			}

			@Test
			@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/keys/someDevice returns 410 for archived vaults")
			public void testUnlockArchived() {
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
		public void testUnlock() {
			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(449);
		}
	}

	@Nested
	@DisplayName("As vault admin user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class CreateVaults {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 201")
		public void testCreateVault1() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3", "metadata1");

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
		public void testCreateVault2() {
			given().contentType(ContentType.JSON)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD") // invalid body (expected json)
					.then().statusCode(400);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100004444 returns 201 ignoring archived flag")
		public void testCreateVault3() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100004444");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 4", true, Instant.parse("2112-12-21T21:12:21Z"), "masterkey4", 42, "NaCl", "authPubKey4", "authPrvKey4", "metadata3");

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
		public void testUpdateVault() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "VaultUpdated", "Vault updated.", true, Instant.parse("2222-11-11T11:11:11Z"), "doNotUpdate", 27, "doNotUpdate", "doNotUpdate", "doNotUpdate", "metadata4");
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
		public void testGrantAccess0() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault1.user1", "user666", "jwe.jwe.jwe.vault1.user666"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 200 for [user998, user999]")
		public void testGrantAccess1() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user998', 'USER', 'User 998');
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
						INSERT INTO "user_details" ("id") VALUES ('user998');
						INSERT INTO "user_details" ("id") VALUES ('user999');
						INSERT INTO "group_membership" ("group_id", "member_id") VALUES ('group2', 'user998');
						INSERT INTO "group_membership" ("group_id", "member_id") VALUES ('group2', 'user999');
						""");
			}

			given().contentType(ContentType.JSON).body(Map.of("user998", "jwe.jwe.jwe.vault1.user998", "user999", "jwe.jwe.jwe.vault1.user999"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'user998';
						DELETE FROM "authority" WHERE "id" = 'user999';
						""");
			}
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 200 for user1")
		public void testGrantAccess2() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault1.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/access-tokens returns 403 (not owning this vault)")
		public void testGrantAccess3() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vault666.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 404 for nonExistingUser")
		public void testGrantAccess4() {
			given().contentType(ContentType.JSON).body(Map.of("user666", "jwe.jwe.jwe.vault1.user666"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 400 for empty body")
		public void testGrantAccess5() {
			given().contentType(ContentType.JSON).body(Map.of())
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-00010000AAAA/access-tokens returns 410")
		public void testGrantAccessArchived() {
			given().contentType(ContentType.JSON).body(Map.of("user1", "jwe.jwe.jwe.vaultAAA.user1"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-00010000AAAA")
					.then().statusCode(410);
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
		public void addNonExistingUser() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user9999")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/users/user2 returns 403 - not owning a nonexisting vault")
		public void addUserToNonExistingVault() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD", "user2")
					.then().statusCode(403);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does not contain user2")
		public void getMembersOfVault2a() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members returns 403")
		public void getMembersOfVault1() {
			when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(403);
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant does contains user2 via group membership")
		public void testGetUsersRequiringAccess1() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET publickey='public2', privatekey='private2', setupcode='setup2'
						WHERE id='user2';
						""");
			}

			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET publickey=NULL, privatekey=NULL, setupcode=NULL
						WHERE id='user2';
						""");
			}
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 201")
		public void testGrantDirectAccessToSelf() {
			given().when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(7)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does contain user2 directly")
		public void getMembersOfVault2b() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
		}

		@Test
		@Order(10)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains user2")
		public void testGetUsersRequiringAccess2() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET publickey='public2', privatekey='private2', setupcode='setup2'
						WHERE id='user2';
						""");
			}

			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));

			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE
						"user_details" SET publickey=NULL, privatekey=NULL, setupcode=NULL
						WHERE id='user2';
						""");
			}
		}

		@Test
		@Order(11)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100002222/access-tokens for user2 returns 200")
		public void testGrantAccess() {
			given().contentType(ContentType.JSON).body(Map.of("user2", "jwe.jwe.jwe.vault2.user2"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains not user2")
		public void testGetUsersRequiringAccess3() {
			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
		}

		@Test
		@Order(13)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 204")
		public void testRevokeAccess() { // previously added in testGrantAccess()
			given().when().delete("/vaults/{vaultId}/authority/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(14)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does not contain user2")
		public void getMembersOfVault2c() {
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

		@BeforeAll
		public void setup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				// user999 will be deleted in #cleanup()
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
						INSERT INTO "user_details" ("id", "publickey", "privatekey", "setupcode") VALUES ('user999', 'public999', 'private999', 'setup999');
						INSERT INTO "group_membership" ("group_id", "member_id") VALUES ('group2', 'user999')
						""");
			}
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group3000 returns 404")
		public void addNonExistingGroup() {
			given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group3000")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 201")
		public void addGroupToVault() {
			given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does contain group2")
		public void getMembersOfVault1a() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("group2"));
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members returns 403")
		public void getMembersOfVault2() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(403);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant contains user999")
		public void testGetUsersRequiringAccess3() {
			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("user999"));
		}

		@Test
		@Order(5)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens for user999 returns 200")
		public void testGrantAccess2() {
			given().contentType(ContentType.JSON).body(Map.of("user999", "jwe.jwe.jwe.vault2.user999"))
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant does no longer contain user999")
		public void testGetUsersRequiringAccess4() {
			given().when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("user999")));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 204")
		public void removeGroup2() {
			given().when().delete("/vaults/{vaultId}/authority/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(204);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does not contain group2")
		public void getMembersOfVault1b() {
			given().when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("group2")));
		}

		@AfterAll
		public void cleanup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE ID = 'user999';
						""");
			}
		}

	}

	@Nested
	@DisplayName("When exceeding 5 seats in license")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ExceedingLicenseLimits {

		@BeforeAll
		public void setup() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('user91', 'USER', 'user name 91'),
							('user92', 'USER', 'user name 92'),
							('user93', 'USER', 'user name 93'),
							('user94', 'USER', 'user name 94'),
							('user95_A', 'USER', 'user name Archived'),
							('group91', 'GROUP', 'group name 91');
							
						INSERT INTO "group_details" ("id")
						VALUES
							('group91');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('user91'),
							('user92'),
							('user93'),
							('user94'),
							('user95_A');
							
						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group91', 'user91'),
							('group91', 'user92'),
							('group91', 'user93'),
							('group91', 'user94');
							
						INSERT INTO "vault_access" ("vault_id", "authority_id")
						VALUES
							('7E57C0DE-0000-4000-8000-00010000AAAA', 'user95_A');
						""");
			}
		}

		@Test
		@Order(0)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 402 for [user91, user92, user93, user94]")
		public void grantAccessExceedingSeats() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 2;
			var body = Map.of(
					"user91", "jwe.jwe.jwe.vault1.user91", //
					"user92", "jwe.jwe.jwe.vault1.user92", //
					"user93", "jwe.jwe.jwe.vault1.user93", //
					"user94", "jwe.jwe.jwe.vault1.user94" //
			);

			given().contentType(ContentType.JSON).body(body)
					.when().post("/vaults/{vaultId}/access-tokens/", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(402);
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group91 returns 402")
		public void addGroupToVaultExceedingSeats() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 2;

			given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group91")
					.then().statusCode(402);
		}

		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/userXX returns 201")
		@ParameterizedTest(name = "Adding user {0} succeeds")
		@CsvSource(value = {"0,user91", "1,user92", "2,user93"})
		public void addUserToVaultNotExceedingSeats(String run, String userId) {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == (2 + Integer.valueOf(run));

			given().when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", userId)
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user94 returns 402")
		public void addUserToVaultExceedingSeats() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 5;

			given().when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", "user94")
					.then().statusCode(402);
		}

		@Test
		@TestSecurity(user = "User 94", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user94")
		})
		@Order(4)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-0001FFFF3333 (as user94) exceeding the license returns 402")
		public void testCreateVaultExceedingSeats() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 5;

			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-0001FFFF3333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 4", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3", "metadata5");
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-0001FFFF3333")
					.then().statusCode(402);
		}

		@Test
		@Order(5)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-0001FFFF3333 (as user1) returns 201 not exceeding seats because user already has access to an existing vault")
		public void testCreateVaultNotExceedingSeats() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 5;

			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-0001FFFF3333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3", "metadata6");
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-0001FFFF3333")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-0001FFFF3333"))
					.body("name", equalTo("My Vault"))
					.body("description", equalTo("Test vault 3"))
					.body("archived", equalTo(false));
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-0001FFFF3333 (as user1) returns 200 with only updated name, description and archive flag, despite exceeding license")
		public void testUpdateVaultDespiteLicenseExceeded() {
			assert EffectiveVaultAccess.countSeatOccupyingUsers() == 5;

			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-0001FFFF3333");
			var vaultDto = new VaultResource.VaultDto(uuid, "VaultUpdated", "Vault updated.", true, Instant.parse("2222-11-11T11:11:11Z"), "someVaule", -1, "doNotUpdate", "doNotUpdate", "doNotUpdate", "metadata7");
			given().contentType(ContentType.JSON)
					.body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-0001FFFF3333")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-0001FFFF3333"))
					.body("name", equalTo("VaultUpdated"))
					.body("description", equalTo("Vault updated."))
					.body("archived", equalTo(true));
		}

		@Test
		@Order(7)
		@DisplayName("unlock/legacyUnlock is granted, if (effective vault user) > license seats but (effective vault user with access token) <= license seat")
		public void testUnlockAllowedExceedingLicenseSoftLimit() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "vault_access" ("vault_id", "authority_id")
							VALUES ('7E57C0DE-0000-4000-8000-000100001111', 'group91');
						""");
			}
			assert EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken() <= 5;

			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200);
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
					.then().statusCode(200)
					.body(is("legacy.jwe.jwe.vault2.device3"));
		}

		@Test
		@Order(8)
		@DisplayName("Unlock/legacyUnlock is blocked if (effective vault users with toke) > license seats")
		public void testUnockBlockedExceedingLicenseHardLimit() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey")
							VALUES ('user91', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user91');
						INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey")
							VALUES ('user92', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user92');
						INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey")
							VALUES ('user93', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user93');
						INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey")
							VALUES ('user94', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user94');
						""");
			}
			assert EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken() > 5;

			when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(402);
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
					.then().statusCode(402);
		}

		@AfterAll
		public void reset() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						DELETE FROM "authority"
						WHERE "id" IN ('user91', 'user92', 'user93', 'user94', 'user95_A', 'group91');
						""");
			}
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
		@Transactional
		public static void setup() throws GeneralSecurityException {
			var keyPairGen = KeyPairGenerator.getInstance("EC");
			keyPairGen.initialize(new ECGenParameterSpec("secp384r1"));
			var keyPair = keyPairGen.generateKeyPair();
			JWT_ALG = Algorithm.ECDSA384((ECPrivateKey) keyPair.getPrivate());

			Vault v = new Vault();
			v.id = UUID.fromString("7E57C0DE-0000-4000-8000-000100009999");
			v.name = "ownership-test-vault";
			v.creationTime = Instant.now();
			v.authenticationPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
			v.metadata = UUID.randomUUID().toString();
			v.persist();
		}

		@Test
		@Order(1)
		@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100009999/claim-ownership returns 400 - JWT has wrong SUB")
		public void testClaimOwnershipIncorrectJWT1() {
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
		public void testClaimOwnershipIncorrectJWT2() {
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
		public void testClaimOwnershipIncorrectJWT3() {
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
		public void testClaimOwnershipIncorrectJWT4() {
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
		public void testClaimOwnershipIncorrectJWT5() {
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
		public void testClaimOwnershipIncorrectJWT6() {
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
		public void testClaimOwnershipIncorrectJWT7() throws GeneralSecurityException {
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
		public void testClaimOwnershipNoSuchVault() {
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
		public void testClaimOwnershipSuccess() {
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
		public void testClaimOwnershipAlreadyClaimed() {
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
		public void reset() throws SQLException {
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
		public void testGetAllVaultsAsUser() {
			when().get("/vaults/all")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("GET /vaults/all returns 200 as user")
		@TestSecurity(user = "User Name 1", roles = {"admin"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testGetAllVaultsAsAdmin() {
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
			public void testListSomeVaults() {
				given().param("ids", "7e57c0de-0000-4000-8000-000100001111", "7e57c0de-0000-4000-8000-000100002222")
						.when().get("/vaults/some")
						.then().statusCode(200)
						.body("id", Matchers.containsInAnyOrder(comparesEqualTo("7e57c0de-0000-4000-8000-000100001111"), comparesEqualTo("7e57c0de-0000-4000-8000-000100002222")));
			}

			@Test
			@DisplayName("GET /vaults/some?ids=7e57c0de-0000-4000-8000-BADBADBADBAD")
			public void testListSomeVaultsNotExistingId() {
				given().param("ids", "7e57c0de-0000-4000-8000-BADBADBADBAD")
						.when().get("/vaults/some")
						.then().statusCode(200)
						.body("", hasSize(0));
			}

			@Test
			@DisplayName("GET /vaults/some")
			public void testListSomeVaultsNoParams() {
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
		public void testListSomeVaultsAsUser() {
			given().param("ids", "7e57c0de-0000-4000-8000-000100001111")
					.when().get("/vaults/some")
					.then().statusCode(403);
		}
	}
}
