package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import org.cryptomator.hub.filters.VaultAdminOnlyFilterProvider;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@QuarkusTest
@DisplayName("Resource /vaults")
public class VaultResourceTest {

	private static String vault1AdminJWT;
	private static String vault2AdminJWT;

	@Inject
	AgroalDataSource dataSource;

	@Inject
	Validator validator;

	@BeforeAll
	public static void beforeAll() throws NoSuchAlgorithmException, InvalidKeySpecException {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

		var algorithmVault1 = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAa57e0Q/KAqmIVOVcWX7b+Sm5YVNRUx8W7nc4wk1IBj2QJmsj+MeShQRHG4ozTE9KhZANiAASVL4lbdVoG9Wv0YpkafXf31YNN3rVD1/BAyZm4EYBg92X+taTvTlBjpaGWZuiSYRW9r+YQdKg1D3zAWb0UEKrOHjkgZ38MbBnTheGLlqH7VspuRWG12zydm0dF1ImiRik="));
		var algorithmVault2 = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU="));

		vault1AdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-000100001111")).withIssuedAt(Instant.now()).sign(algorithmVault1);
		vault2AdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-000100002222")).withIssuedAt(Instant.now()).sign(algorithmVault2);
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

		@Test
		public void testValidDto() {
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, 8, VALID_SALT, VALID_AUTH_PUB, VALID_AUTH_PRI);
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
		@DisplayName("GET /vaults returns 200")
		public void testGetSharedOrOwned() {
			when().get("/vaults")
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
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members returns 401")
		public void testGetAccess() {
			when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/logged-in-user returns 200 using user access")
		public void testUnlock1() {
			when().get("/vaults/{vaultId}/access-tokens/logged-in-user", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault1.user1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/access-tokens/logged-in-user returns 200 using group access")
		public void testUnlock2() {
			when().get("/vaults/{vaultId}/access-tokens/logged-in-user", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body(is("jwe.jwe.jwe.vault2.user1"));
		}

	}

	@Nested
	@DisplayName("As vault admin user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsVaultAdmin {

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members returns 200")
		public void testGetAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members returns 400 when using incorrect 'Cryptomator-Vault-Admin-Authorization' header")
		public void testGetAccess2() {
			var vault2ButWrongKeyAdminJWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.cGZDCqzJQgcBHNVPcmBc8JfeGzUf3CHUrwSAMwOA0Dcy9aUZvsAm1dr1MKzuPW_UFHRfMnNi2EwASOA6t-vPWvPFolAHFn5REt2Y9Aw9mIz-qxSBLpz6OMZD16tysQcd";

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2ButWrongKeyAdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111 returns 409")
		public void testCreateVault1() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100001111");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 1", Instant.parse("1999-11-19T19:19:19Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", uuid.toString())
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 201")
		public void testCreateVault3() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD returns 400 due to malformed request body")
		public void testCreateVault4() {
			given().contentType(ContentType.JSON)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD") // invalid body (expected json)
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/user999 returns 201")
		public void testGrantAccess1() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
					INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
					INSERT INTO "user_details" ("id") VALUES ('user999');
					INSERT INTO "group_membership" ("group_id", "member_id") VALUES ('group2', 'user999')
					""");
			}

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault1.user999")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-000100001111", "user999")
					.then().statusCode(201);

			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						DELETE FROM "authority" WHERE "id" = 'user999';
						""");
			}
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/user1 returns 409 due to user access already granted")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault1.user1")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-000100001111", "user1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/access-tokens/user1 returns 400 (vault admin jwt can not be checked for nonexisting vault)")
		public void testGrantAccess3() {
			given()
					.header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault666.user1")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD", "user1")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/nonExistingUser returns 404 (no such user)")
		public void testGrantAccess4() {
			given()
					.header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault2.user666")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-000100001111", "nonExistingUser")
					.then().statusCode(404);
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
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user9999")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD/users/user2 returns 404 - no such vault")
		public void addUserToNonExistingVault() throws NoSuchAlgorithmException, InvalidKeySpecException {
			var algorithmVault = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU="));
			var vaultAdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-BADBADBADBAD")).withIssuedAt(Instant.now()).sign(algorithmVault);

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vaultAdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD", "user2")
					.then().statusCode(404);
		}

		@Test
		@Order(3)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/users/user9999 returns 401 - unauthenticated")
		public void addNonExistingUserUnauthenticated() {
			when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user9999")
					.then().statusCode(401);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does not contain user2")
		public void getAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant does not contains device2")
		public void testGetUsersRequiringAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 201")
		public void testGrantAccess() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(7)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/members does contain user2")
		public void getMembers2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
		}

		@Test
		@Order(8)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/users/user2 returns 409 - user2 already direct member of vault2")
		public void testGrantAccessAgain() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(409);
		}

		@Test
		@Order(9)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/groups/group1 returns 409 - group1 already direct member of vault2")
		public void testAddingMemberAgainFails() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100002222", "group1")
					.then().statusCode(409);
		}

		@Test
		@Order(10)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains device2")
		public void testGetUsersRequiringAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
		}

		@Test
		@Order(11)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/access-tokens/user2 returns 201")
		public void testGrantAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.given().contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault2.device2")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/users-requiring-access-grant contains not user2")
		public void testGetUsersRequiringAccess3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
		}

		@Test
		@Order(13)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 204")
		public void testRevokeAccess() { // previously added in testGrantAccess()
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().delete("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(14)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/access does not contain user2")
		public void getMembers3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100002222")
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
			try (var s = dataSource.getConnection().createStatement()) {
				// user999 will be deleted in #cleanup()
				s.execute("""
					INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
					INSERT INTO "user_details" ("id") VALUES ('user999');
					INSERT INTO "group_membership" ("group_id", "member_id") VALUES ('group2', 'user999')
					""");
			}
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group3000 returns 404")
		public void addNonExistingGroup() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group3000")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 201")
		public void addGroupToVault() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does contain group2")
		public void getMembers1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("group2"));
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant contains user999")
		public void testGetUsersRequiringAccess3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("user999"));
		}

		@Test
		@Order(5)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/user999 returns 201")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault2.user999")
					.when().put("/vaults/{vaultId}/access-tokens/{userId}", "7E57C0DE-0000-4000-8000-000100001111", "user999")
					.then().statusCode(201);
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant does no longer contain user999")
		public void testGetUsersRequiringAccess4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/users-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("user999")));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group2 returns 204")
		public void removeGroup2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().delete("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group2")
					.then().statusCode(204);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/members does not contain group2")
		public void getMembers2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/members", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("group2")));
		}

		@AfterAll
		public void cleanup() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
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
			//Assumptions.assumeTrue(EffectiveVaultAccess.countEffectiveVaultUsers() == 2);
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('user91', 'USER', 'user name 91'),
							('user92', 'USER', 'user name 92'),
							('user93', 'USER', 'user name 93'),
							('user94', 'USER', 'user name 94'),
							('group91', 'GROUP', 'group name 91');
							
						INSERT INTO "group_details" ("id")
						VALUES
							('group91');
							
							
						INSERT INTO "user_details" ("id")
						VALUES
							('user91'),
							('user92'),
							('user93'),
							('user94');
							
						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group91', 'user91'),
							('group91', 'user92'),
							('group91', 'user93'),
							('group91', 'user94');
							
						""");
			}
		}

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/groups/group91 returns 402")
		public void addGroupToVaultExceedingSeats() {
			//Assumptions.assumeTrue(EffectiveVaultAccess.countEffectiveVaultUsers() == 2);

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group91")
					.then().statusCode(402);
		}

		@Order(2)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/userXX returns 201")
		@ParameterizedTest(name = "Adding user {index} succeeds")
		@ValueSource(strings = {"user91", "user92", "user93"})
		public void addUserToVaultNotExceedingSeats(String userId) {
			//Assumptions.assumeTrue(EffectiveVaultAccess.countEffectiveVaultUsers() == 2);

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", userId)
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user94 returns 402")
		public void addUserToVaultExceedingSeats() {
			//Assumptions.assumeTrue(EffectiveVaultAccess.countEffectiveVaultUsers() == 5);

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", "user94")
					.then().statusCode(402);
		}

		@Test
		@Order(4)
		@DisplayName("Unlock is blocked if exceeding license seats")
		public void testUnlockBlockedIfLicenseExceeded() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "vault_access" ("vault_id", "authority_id")
							VALUES ('7E57C0DE-0000-4000-8000-000100001111', 'group91');
						""");
			}
			//Assumptions.assumeTrue(EffectiveVaultAccess.countEffectiveVaultUsers() > 5);

			when().get("/vaults/{vaultId}/access-tokens/logged-in-user", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(402);
		}

		@AfterAll
		public void reset() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						DELETE FROM "authority"
						WHERE "id" IN ('user91', 'user92', 'user93', 'user94', 'group91');
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
				"GET, /vaults",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/members",
				"PUT, /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user1",
				"DELETE, /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user1",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/users-requiring-access-grant",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens/logged-in-user"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

}