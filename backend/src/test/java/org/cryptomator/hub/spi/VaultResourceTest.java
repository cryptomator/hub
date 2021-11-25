package org.cryptomator.hub.spi;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(VaultResource.class)
public class VaultResourceTest {

	@Test
	@TestSecurity(user = "userName1", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "userId1")
	})
	public void testGetDeviceSpecificMasterKey() {
		given()
				.when()
				.get("vaultId1/keys/noSuchDevice")
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "userName2", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "userId2")
	})
	public void testCreateVault() {
		var uuid = "uuid1";
		var name = "name1";
		var masterkey = "masterkey1";
		var iterations = "iterations1";
		var salt = "salt1";

		var vaultDto = new VaultResource.VaultDto(uuid, name, masterkey, iterations, salt);

		given()
				.when()
				.contentType(ContentType.JSON)
				.body(vaultDto)
				.put(uuid)
				.then()
				.statusCode(201);
	}
}