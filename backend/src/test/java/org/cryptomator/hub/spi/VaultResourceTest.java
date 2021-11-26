package org.cryptomator.hub.spi;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

@QuarkusTest
@DisplayName("Resource /vaults")
public class VaultResourceTest {

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Nested
	@DisplayName("As user1")
	@TestSecurity(user = "User Name 1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsAuthorzedUser1 {

		@Test
		@DisplayName("GET /vaults/vault1/keys/device1 returns 200")
		public void testUnlock1() {
			when().get("/vaults/vault1/keys/device1")
					.then().statusCode(200)
					.body("device_specific_masterkey", CoreMatchers.is("dsm1"))
					.body("ephemeral_public_key", CoreMatchers.is("epk1"));
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/noSuchDevice returns 404")
		public void testUnlock2() {
			when().get("/vaults/vault1/keys/noSuchDevice")
					.then().statusCode(404);
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/device2 returns 403")
		public void testUnlock3() {
			when().get("/vaults/vault1/keys/device2")
					.then().statusCode(403);
		}

	}

	@Nested
	@DisplayName("As vault owner user1")
	@TestSecurity(user = "User Name 1", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsVaultOwner {

		@Test
		@DisplayName("PUT /vaults/vault1 returns 409")
		public void testCreateVault1() {
			var vaultDto = new VaultResource.VaultDto("vault1", "My Vault", "masterkey3", "42", "NaCl");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3 returns 201")
		public void testCreateVault2() {
			var vaultDto = new VaultResource.VaultDto("vault3", "My Vault", "masterkey3", "42", "NaCl");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault3")
					.then().statusCode(201);
		}

	}

	@Nested
	@DisplayName("As unauthenticated user")
	public class AsAnonymous {

		@Test
		@DisplayName("GET /vaults/vault1/keys/device1 returns 401")
		public void testUnlock1() {
			when().get("/vaults/vault1/keys/device1")
					.then().statusCode(401);
		}

	}

}