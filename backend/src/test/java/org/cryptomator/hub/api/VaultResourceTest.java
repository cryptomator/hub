package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.cryptomator.hub.filters.VaultAdminOnlyFilterProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;
import javax.validation.Validator;
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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
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

		vault1AdminJWT = JWT.create().withHeader(Map.of("vaultId", "vault1")).withIssuedAt(Instant.now()).sign(algorithmVault1);
		vault2AdminJWT = JWT.create().withHeader(Map.of("vaultId", "vault2")).withIssuedAt(Instant.now()).sign(algorithmVault2);
	}

	private static PrivateKey getPrivateKey(String keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBytes)));
	}

	@Nested
	@DisplayName("Test VaultDto validation")
	public class TestVaultDtoValidation {

		private static final String VALID_ID = "2535b22b-a786-4b0f-947d-bae104c4f14f";
		private static final String VALID_NAME = "foobar";
		private static final String VALID_MASTERKEY = "base64";
		private static final String VALID_SALT = "base64";
		private static final String VALID_AUTH_PUB = "base64";
		private static final String VALID_AUTH_PRI = "base64";

		@Test
		public void testValidDto() {
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, "8", VALID_SALT, VALID_AUTH_PUB, VALID_AUTH_PRI);
			var violations = validator.validate(dto);
			MatcherAssert.assertThat(violations, Matchers.empty());
		}

		@ParameterizedTest
		@DisplayName("Testing invalid values for property iterations")
		@ValueSource(strings = {"foo", "-5", "0x20", "10e10", "0.33"})
		@NullAndEmptySource
		public void testInvalidIterationss(String iterations) {
			var dto = new VaultResource.VaultDto(VALID_ID, VALID_NAME, "foobarbaz", Instant.parse("2020-02-20T20:20:20Z"), VALID_MASTERKEY, iterations, VALID_SALT, VALID_AUTH_PUB, VALID_AUTH_PRI);
			var violations = validator.validate(dto);
			MatcherAssert.assertThat(violations, Matchers.not(Matchers.empty()));
		}

	}

	@Nested
	@DisplayName("Test PUT /vaults/users/{userId} endpoint (addUser)")
	public class TestAddUser {

		@Test
		@DisplayName("If all license Seats are used, block new user with 402")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testDepletedLicenseBlocksNewUser() throws SQLException {
			//this test depends on the flyway migration Test_Data
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('newUser91', 'USER', 'newUser91 name'),
							('newUser92', 'USER', 'newUser92 name'),
							('newUser93', 'USER', 'newUser93 name'),
							('newUser94', 'USER', 'newUser94 name');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('newUser91'),
							('newUser92'),
							('newUser93'),
							('newUser94');
							
						INSERT INTO "vault_access" ("vault_id", "authority_id")
						VALUES
							('vault1', 'newUser91'),
							('vault1', 'newUser92'),
							('vault1', 'newUser93');
						""");
			}
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "vault1", "newUser94")
					.then().statusCode(402);
		}

		@Test
		@DisplayName("If all license Seats are used, allow users having any vault access")
		@TestSecurity(user = "User Name 2", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user2")
		})
		public void testDepletedLicenseAllowsUsingUser() throws SQLException {
			//this test depends on the flyway migration Test_Data
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('newUser91', 'USER', 'newUser91 name'),
							('newUser92', 'USER', 'newUser92 name'),
							('newUser93', 'USER', 'newUser93 name');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('newUser91'),
							('newUser92'),
							('newUser93');
							
						INSERT INTO "vault_access" ("vault_id", "authority_id")
						VALUES
							('vault1', 'newUser91'),
							('vault1', 'newUser92'),
							('vault1', 'newUser93');
						""");
			}
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "vault2", "newUser91")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("Adding user, who is already direct member of the vault, returns 409")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testAddingDirectMemberAgainFails() throws SQLException {
			//this test depends on the flyway migration Test_Data
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().put("/vaults/{vaultId}/users/{usersId}", "vault1", "user1")
					.then().statusCode(409);
		}

	}


	@Nested
	@DisplayName("Test PUT /vaults/groups/{userId} endpoint (addGroup)")
	public class TestAddGroup {

		@Test
		@DisplayName("Adding group, which is already direct member of the vault, returns 409")
		@TestSecurity(user = "User Name 2", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user2")
		})
		public void testAddingMemberAgainFails() throws SQLException {
			//this test depends on the flyway migration Test_Data
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group1")
					.then().statusCode(409);
		}

	}

	@Nested
	@DisplayName("Test GET /vaults/{vaultId]/keys/{deviceId} endpoint (unlock)")
	public class TestUnlock {

		@Test
		@DisplayName("Unlock is blocked if there are more EVUs than license seats")
		@TestSecurity(user = "User Name 1", roles = {"user"})
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user1")
		})
		public void testUnlockBlockedIfEVUsExcceedLicense() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('newUser91', 'USER', 'newUser91 name'),
							('newUser92', 'USER', 'newUser92 name'),
							('newUser93', 'USER', 'newUser93 name'),
							('newUser94', 'USER', 'newUser94 name');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('newUser91'),
							('newUser92'),
							('newUser93'),
							('newUser94');
							
						INSERT INTO "vault_access" ("vault_id", "authority_id")
						VALUES
							('vault1', 'newUser91'),
							('vault1', 'newUser92'),
							('vault1', 'newUser93'),
							('vault1', 'newUser94');
						""");
			}

			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device1")
					.then().statusCode(402);
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
					.body("size()", is(2))
					.body("id", hasItems("vault1", "vault2"));
		}

		@Test
		@DisplayName("GET /vaults/vault1 returns 200")
		public void testGetVault1() {
			when().get("/vaults/{vaultId}", "vault1")
					.then().statusCode(200)
					.body("id", is("vault1"));
		}

		@Test
		@DisplayName("GET /vaults/nonExistingVault returns 404")
		public void testGetVault2() {
			when().get("/vaults/{vaultId}", "nonExistingVault")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/vault1/members returns 401")
		public void testGetAccess() {
			when().get("/vaults/{vaultId}/members", "vault1")
					.then().statusCode(401);
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/device1 returns 200 using user access")
		public void testUnlock1() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device1")
					.then().statusCode(200)
					.body(is("jwe1"));
		}

		@Test
		@DisplayName("GET /vaults/vault2/keys/device3 returns 200 using group access")
		public void testUnlock2() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device3")
					.then().statusCode(200)
					.body(is("jwe3"));
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/noSuchDevice returns 404")
		public void testUnlock3() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault1", "noSuchDevice")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/device2 returns 403")
		public void testUnlock4() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device2")
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
		@DisplayName("GET /vaults/vault1/members returns 200")
		public void testGetAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2"))
					.and().body("type", not(hasItems("GROUP")));
		}

		@Test
		@DisplayName("GET /vaults/vault2/members returns 400")
		public void testGetAccess2() {
			var vault2ButWrongKeyAdminJWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.cGZDCqzJQgcBHNVPcmBc8JfeGzUf3CHUrwSAMwOA0Dcy9aUZvsAm1dr1MKzuPW_UFHRfMnNi2EwASOA6t-vPWvPFolAHFn5REt2Y9Aw9mIz-qxSBLpz6OMZD16tysQcd";

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2ButWrongKeyAdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/vault1 returns 409")
		public void testCreateVault1() {
			var vaultDto = new VaultResource.VaultDto("vault1", "My Vault", "Test vault 1", Instant.parse("1999-11-19T19:19:19Z"), "masterkey3", "42", "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vaultX returns 409")
		public void testCreateVault2() {
			var vaultDto = new VaultResource.VaultDto("vaultX", "Vault 1", "This is a testvault.", Instant.parse("2020-02-20T20:20:20Z"), "masterkey1", "42", "salt1", "authPubKey1", "authPrvKey1");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vaultX")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3 returns 201")
		public void testCreateVault3() {
			var vaultDto = new VaultResource.VaultDto("vault3", "My Vault", "Test vault 3", Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", "42", "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault999")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/vault999 returns 400")
		public void testCreateVault4() {
			given().contentType(ContentType.JSON)
					.when().put("/vaults/{vaultId}", "vault999")
					.then().statusCode(400);
		}

		@Test
		@DisplayName("PUT /vaults/vault1/keys/device3 returns 201")
		public void testGrantAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe.jwe.jwe.jwe.9999")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device3")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/vault1/keys/device1 returns 409 due to user access already granted")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe1.jwe1.jwe1.jwe1.jwe1")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault2/keys/device3 returns 409 due to group access already granted")
		@TestSecurity(user = "User Name 2", roles = {"user"}) //we switch here for easy usage
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user2")
		})
		public void testGrantAccess3() throws SQLException {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.contentType(ContentType.TEXT).body("jwe3.jwe3.jwe3.jwe3.jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device3")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault1/keys/nonExistingDevice returns 404")
		public void testGrantAccess4() {
			given()
					.header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault1AdminJWT)
					.contentType(ContentType.TEXT).body("jwe3.jwe3.jwe3.jwe3.jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "nonExistingDevice")
					.then().statusCode(404);
		}

	}

	@Nested
	@DisplayName("Managing members as user2")
	@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"}, clean = false)
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageMembers {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/vault2/users/user9999 returns 404")
		public void addNonExistingUser() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "vault2", "user9999")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/vault9999/users/user2 returns 404")
		public void addUserToNonExistingVault() throws NoSuchAlgorithmException, InvalidKeySpecException {
			var algorithmVault = Algorithm.ECDSA384((ECPrivateKey) getPrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU="));
			var vaultAdminJWT = JWT.create().withHeader(Map.of("vaultId", "vault9999")).withIssuedAt(Instant.now()).sign(algorithmVault);

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vaultAdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "vault9999", "user2")
					.then().statusCode(404);
		}

		@Test
		@Order(3)
		@DisplayName("PUT /vaults/vault2/users/user9999 returns 401")
		public void addNonExistingUserUnauthenticated() {
			when().put("/vaults/{vaultId}/users/{userId}", "vault2", "user9999")
					.then().statusCode(401);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/vault2/members does not contain user2")
		public void getAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant does not contains device2")
		public void testGetDevicesRequiringAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/vault2/members/user2 returns 201")
		public void addUser1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/users/{userId}", "vault2", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(7)
		@DisplayName("GET /vaults/vault2/members does contain user2")
		public void getMembers2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains device2")
		public void testGetDevicesRequiringAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device2"));
		}

		@Test
		@Order(9)
		@DisplayName("PUT /vaults/vault2/keys/device2 returns 201")
		public void testGrantAccess1() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.given().contentType(ContentType.TEXT).body("jwe.jwe.jwe.jwe.9999")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device2")
					.then().statusCode(201);
		}

		@Test
		@Order(10)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device2")
		public void testGetDevicesRequiringAccess3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(11)
		@DisplayName("PUT /devices/device9999 returns 201")
		public void testCreateDevice2() {
			var deviceDto = new DeviceResource.DeviceDto("device9999", "Computer 9999", "publickey9999", "user2", Set.of(), Instant.parse("2020-02-20T20:20:20Z"));

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device9999")
					.then().statusCode(201);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device9999")
		public void testGetDevicesRequiringAccess4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device9999"));
		}

		@Test
		@Order(13)
		@DisplayName("DELETE /vaults/vault2/members/user2 returns 204")
		public void removeUser2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().delete("/vaults/{vaultId}/users/{userId}", "vault2", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(14)
		@DisplayName("GET /vaults/vault2/access does not contain user2")
		public void getMembers3() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
		}
	}

	@Nested
	@DisplayName("Managing groups as user2")
	@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), clean = false)
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageGroups {

		private final String vault4AdminJWT = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDQifQ.eyJpYXQiOjE1MTYyMzkwMTUsImV4cCI6NTgxNjIzOTAzMCwibmJmIjoxNTE2MjM5MDAwfQ.aSe-P9j1vybUPS_Y1PDQ5knh_slaNulX365TSJNBYGY5dooUTZicu27_h_jYC-vvsafn4sjQhifEoXJq1U5r2kEp8ZV_Bn_4GdLLAG2JDewuAvWlkqks1un4spEO8yO4";

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/vault4/groups/group3000 returns 404")
		public void addNonExistingGroup() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault4AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "vault4", "group3000")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("PUT /vaults/vault2/groups/group91 returns 201")
		public void addGroupToVault() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('group91', 'GROUP', 'group name 91');
							
						INSERT INTO "group_details" ("id")
						VALUES
							('group91');
							
						""");
			}

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group91")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/vault2/members does contain group91")
		public void getMembers4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("group91"));
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains device93")
		public void testGetDevicesRequiringAccess3() throws SQLException {
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('user90', 'USER', 'user name 90');
							
						INSERT INTO "user_details" ("id")
						VALUES
							('user90');
							
						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group91', 'user90');
							
						INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time")
						VALUES
							('device93', 'user90', 'Computer 9', 'publickey90', '2020-02-20 20:20:20');
						""");
			}

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device93"));
		}

		@Test
		@Order(5)
		@DisplayName("PUT /vaults/vault2/keys/device93 returns 201")
		public void testGrantAccess2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.contentType(ContentType.TEXT).body("aaa.AAA.000.999.888")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device93")
					.then().statusCode(201);
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device93")
		public void testGetDevicesRequiringAccess4() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device93")));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /vaults/vault2/groups/group91 returns 204")
		public void removeGroup2() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().delete("/vaults/{vaultId}/groups/{groupId}", "vault2", "group91")
					.then().statusCode(204);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/vault2/members does not contain group91")
		public void getMembers5() {
			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("group91")));
		}

		@Test
		@Order(9)
		@DisplayName("PUT /vaults/vault2/groups/group94 returns 402")
		public void addGroupToVaultExceedingSeats() throws SQLException {
			//license has 5 seats, two are already used
			try (var s = dataSource.getConnection().createStatement()) {
				s.execute("""
						INSERT INTO "authority" ("id", "type", "name")
						VALUES
							('group94', 'GROUP', 'group name 94'),
							('user95', 'USER', 'user name 95'),
							('user96', 'USER', 'user name 96'),
							('user97', 'USER', 'user name 97'),
							('user98', 'USER', 'user name 98');
							
						INSERT INTO "group_details" ("id")
						VALUES
							('group94');
							
							
						INSERT INTO "user_details" ("id")
						VALUES
							('user95'),
							('user96'),
							('user97'),
							('user98');
							
						INSERT INTO "group_membership" ("group_id", "member_id")
						VALUES
							('group94', 'user95'),
							('group94', 'user96'),
							('group94', 'user97'),
							('group94', 'user98'),
							('group94', 'user1');
							
						""");
			}

			given().header(VaultAdminOnlyFilterProvider.VAULT_ADMIN_AUTHORIZATION, vault2AdminJWT)
					.when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group94")
					.then().statusCode(402);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@DisplayName("401 Unauthorized")
		@ParameterizedTest(name = "{0} {1}")
		@CsvSource(value = {
				"GET, /vaults",
				"GET, /vaults/vault1",
				"GET, /vaults/vault1/members",
				"PUT, /vaults/vault1/users/user1",
				"DELETE, /vaults/vault1/users/user1",
				"GET, /vaults/vault1/devices-requiring-access-grant",
				"GET, /vaults/vault1/keys/device1"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

}