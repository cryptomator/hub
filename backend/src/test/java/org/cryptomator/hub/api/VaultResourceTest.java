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
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@QuarkusTest
@DisplayName("Resource /vaults")
public class VaultResourceTest {

	private static String vault1AdminJWT;
	private static String vault2AdminJWT;
	private static String vaultArchivedAdminJWT;

	@Inject
	AgroalDataSource dataSource;

	@Inject
	Validator validator;

	@BeforeAll
	public static void beforeAll() throws NoSuchAlgorithmException, InvalidKeySpecException {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

		var algorithmVault1 = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAa57e0Q/KAqmIVOVcWX7b+Sm5YVNRUx8W7nc4wk1IBj2QJmsj+MeShQRHG4ozTE9KhZANiAASVL4lbdVoG9Wv0YpkafXf31YNN3rVD1/BAyZm4EYBg92X+taTvTlBjpaGWZuiSYRW9r+YQdKg1D3zAWb0UEKrOHjkgZ38MbBnTheGLlqH7VspuRWG12zydm0dF1ImiRik="));
		var algorithmVault2 = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU="));
		var algorithmVaultArchived = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU="));

		vault1AdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-000100001111")).withIssuedAt(Instant.now()).sign(algorithmVault1);
		vault2AdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-000100002222")).withIssuedAt(Instant.now()).sign(algorithmVault2);
		vaultArchivedAdminJWT = JWT.create().withHeader(Map.of("vaultId", "7E57C0DE-0000-4000-8000-AAAAAAAAAAAA")).withIssuedAt(Instant.now()).sign(algorithmVaultArchived);
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
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, 8, VALID_SALT, VALID_AUTH_PUB, VALID_AUTH_PRI, false);
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
		public void testGetSharedOrOwnedNotArchived() {
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
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device1 returns 200 using user access")
		public void testUnlock1() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device1")
					.then().statusCode(200)
					.body(is("jwe1"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/keys/device3 returns 200 using group access")
		public void testUnlock2() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "device3")
					.then().statusCode(200)
					.body(is("jwe3"));
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/noSuchDevice returns 404")
		public void testUnlock3() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "noSuchDevice")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device2 returns 403")
		public void testUnlock4() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device2")
					.then().statusCode(403);
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
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 1", Instant.parse("1999-11-19T19:19:19Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3", false);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", uuid.toString())
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222 returns 409")
		public void testCreateVault2() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100002222");
			var vaultDto = new VaultResource.VaultDto(uuid, "Vault 1", "This is a testvault.", Instant.parse("2020-02-20T20:20:20Z"), "masterkey1", 42, "salt1", "authPubKey1", "authPrvKey1", false);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", uuid.toString())
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100003333 returns 201")
		public void testCreateVault3() {
			var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100003333");
			var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 3", Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3", false);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-BADBADBADBAD returns 400")
		public void testCreateVault4() {
			given().contentType(ContentType.JSON)
					.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-BADBADBADBAD")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device3 returns 201")
		public void testGrantAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault1.device3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device3")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device1 returns 409 due to user access already granted")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe1.jwe1.jwe1.jwe1.jwe1")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/keys/device3 returns 409 due to group access already granted")
		@TestSecurity(user = "User Name 2", roles = {"user"}) //we switch here for easy usage
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user2")
		})
		public void testGrantAccess3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.contentType(ContentType.TEXT).body("jwe3.jwe3.jwe3.jwe3.jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "device3")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/nonExistingDevice returns 404")
		public void testGrantAccess4() {
			given()
					.header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe3.jwe3.jwe3.jwe3.jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "nonExistingDevice")
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
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/devices-requiring-access-grant does not contains device2")
		public void testGetDevicesRequiringAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
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
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/devices-requiring-access-grant contains device2")
		public void testGetDevicesRequiringAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("device2"));
		}

		@Test
		@Order(11)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100002222/keys/device2 returns 201")
		public void testGrantAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.given().contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault2.device2")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "device2")
					.then().statusCode(201);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/devices-requiring-access-grant contains not device2")
		public void testGetDevicesRequiringAccess3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(13)
		@DisplayName("PUT /devices/device9999 returns 201")
		public void testCreateDevice2() {
			var deviceDto = new DeviceResource.DeviceDto("device9999", "Computer 9999", "publickey9999", "user2", Set.of(), Instant.parse("2020-02-20T20:20:20Z"));

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device9999")
					.then().statusCode(201);
		}

		@Test
		@Order(14)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100002222/devices-requiring-access-grant contains not device9999")
		public void testGetDevicesRequiringAccess4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100002222")
					.then().statusCode(200)
					.body("id", hasItems("device9999"));
		}

		@Test
		@Order(15)
		@DisplayName("DELETE /vaults/7E57C0DE-0000-4000-8000-000100002222/members/user2 returns 204")
		public void testRevokeAccess() { // previously added in testGrantAccess()
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().delete("/vaults/{vaultId}/users/{userId}", "7E57C0DE-0000-4000-8000-000100002222", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(16)
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
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/devices-requiring-access-grant contains device999")
		public void testGetDevicesRequiringAccess3() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				// device999 will be deleted in #cleanup()
				s.execute("""
						INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time")
						VALUES
							('device999', 'user2', 'Computer 999', 'publickey90', '2020-02-20 20:20:20');
						""");
			}

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", hasItems("device999"));
		}

		@Test
		@Order(5)
		@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device999 returns 201")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.vault2.device93")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device999")
					.then().statusCode(201);
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/7E57C0DE-0000-4000-8000-000100001111/devices-requiring-access-grant contains not device999")
		public void testGetDevicesRequiringAccess4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "7E57C0DE-0000-4000-8000-000100001111")
					.then().statusCode(200)
					.body("id", not(hasItems("device999")));
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
						DELETE FROM "device" WHERE ID = 'device999';
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
							('userAA', 'USER', 'user name Archived'),
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
							('userAA');
							
						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group91', 'user91'),
							('group91', 'user92'),
							('group91', 'user93'),
							('group91', 'user94');
							
						INSERT INTO "vault_access" ("vault_id", "authority_id")
						VALUES
							('7E57C0DE-0000-4000-8000-AAAAAAAAAAAA', 'userAA');
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

			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100001111", "device1")
					.then().statusCode(402);
		}

		@AfterAll
		public void reset() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						DELETE FROM "authority"
						WHERE "id" IN ('user91', 'user92', 'user93', 'user94', 'userAA', 'group91');
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
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/devices-requiring-access-grant",
				"GET, /vaults/7E57C0DE-0000-4000-8000-000100001111/keys/device1"
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
					.body("id", hasItems(equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100001111"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100002222"), equalToIgnoringCase("7E57C0DE-0000-4000-8000-AAAAAAAAAAAA")));
		}
	}

	@Nested
	@DisplayName("GET /vaults/{vaultid}/keys/{deviceId}")
	public class Unlock {

		@Test
		@DisplayName("GET /vaults/{vaultid}/keys/{deviceId} returns 404 for not-existing vaults")
		@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testUnlockNotExistingVault() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-FFFFFFFFFFFF", "someDevice")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/{vaultid}/keys/{deviceId} returns 403 for archived vaults")
		@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testUnlockArchived() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-AAAAAAAAAAAA", "someDevice")
					.then().statusCode(403);
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

	@Nested
	@DisplayName("PATCH /vaults/{vaultid}")
	@TestSecurity(user = "User Name 1", roles = {"user", "admin"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	public class Update {

		@BeforeAll
		public void insertData() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "vault" ("id", "name", "description", "creation_time", "salt", "iterations", "masterkey", "auth_pubkey", "auth_prvkey", "archived")
						VALUES
						('7E57C0DE-0000-4000-8000-DADADADADADA', 'Vault U', 'Vault to update.',
						 '2020-02-20 20:20:20', 'saltU', 42, 'masterkeyU', 'auth_pubkeyU', 'auht_prvkeyU', FALSE),
						('7E57C0DE-0000-4000-8000-ADADADADADAD', 'Vault R', 'Vault to update2.',
						 '2020-02-20 20:20:20', 'saltR', 42, 'masterkeyR', 'auth_pubkeyR', 'auht_prvkeyR', TRUE);
						""");

			}
		}

		@Test
		@DisplayName("PATCH /vaults/{vaultId}} with Body { \"name\":\"Vault X\", \"description\":\"Vault updated\", \"archived\":true } returns 200 with updated name, description and archive status")
		public void testUpdateAll() {
			given().contentType(ContentType.JSON)
					.body(new VaultResource.VaultUpdateDto("Vault X", "Vault updated", "true"))
					.when().patch("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-DADADADADADA")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-DADADADADADA"))
					.body("name", equalTo("Vault X"))
					.body("description", equalTo("Vault updated"))
					.body("archived", equalTo(true));
		}

		@Test
		@DisplayName("PATCH /vaults/{vaultId}} with Body { \"name\":\"Vault Y\"} returns 200 with only updated name")
		public void testUpdateOnlyName() {
			given().contentType(ContentType.JSON)
					.body(new VaultResource.VaultUpdateDto("Vault Y", null, null))
					.when().patch("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-ADADADADADAD")
					.then().statusCode(200)
					.body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-ADADADADADAD"))
					.body("name", equalTo("Vault Y"))
					.body("description", equalTo("Vault to update2."))
					.body("archived", equalTo(true));
		}

		@Test
		@DisplayName("PATCH /vaults/{vaultId}} with Body { \"archived\":\"yodelDodel\"} returns 400 ")
		public void testUpdateBadRequest() {
			given().contentType(ContentType.JSON)
					.body(new VaultResource.VaultUpdateDto(null, null, "yodelDodel"))
					.when().patch("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-ADADADADADAD")
					.then().statusCode(400);
		}

		@AfterAll
		public void deleteData() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						DELETE FROM "vault"
						WHERE "id" IN ('7E57C0DE-0000-4000-8000-DADADADADADA','7E57C0DE-0000-4000-8000-ADADADADADAD');
						""");
			}
		}

	}
}