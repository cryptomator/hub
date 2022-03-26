package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Timestamp;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
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
	public class AsAuthorizedUser1 {

		@Test
		@DisplayName("GET /vaults returns 200")
		public void testGetSharedOrOwned() {
			when().get("/vaults")
					.then().statusCode(200)
					.body("size()", is(4))
					.body("id", hasItems("vault1", "vault2", "vault3"));
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
		@DisplayName("GET /vaults/vault1/access returns 403")
		public void testGetAccess() {
			when().get("/vaults/{vaultId}/access", "vault1")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("GET /vaults/vault1/keys/device1 returns 200 using user access")
		public void testUnlock1() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device1")
					.then().statusCode(200)
					.body(is("jwe1"));
		}

		@Test
		@DisplayName("GET /vaults/vault3/keys/device6 returns 200 using group access")
		public void testUnlock2() {
			when().get("/vaults/{vaultId}/keys/{deviceId}", "vault3", "device6")
					.then().statusCode(200)
					.body(is("jwe5"));
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
	@DisplayName("As vault owner user1")
	@TestSecurity(user = "User Name 1", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsVaultOwner {

		@Test
		@DisplayName("GET /vaults/vault1/access returns 200")
		public void testGetAccess1() {
			when().get("/vaults/{vaultId}/access", "vault1")
					.then().statusCode(200)
					.body("users.id", hasItems("user1", "user2"))
					.body("groups.id", empty());
		}

		@Test
		@DisplayName("GET /vaults/vault3/access returns 200")
		public void testGetAccess2() {
			when().get("/vaults/{vaultId}/access", "vault3")
					.then().statusCode(200)
					.body("users.id", empty())
					.body("groups.id", hasItems("group1"));
		}

		@Test
		@DisplayName("PUT /vaults/vault1 returns 409")
		public void testCreateVault1() {
			var vaultDto = new VaultResource.VaultDto("vault1", "My Vault","Test vault 1", Timestamp.valueOf("1999-11-19 19:19:19"), null, "masterkey3", "42", "NaCl");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vaultX returns 409")
		public void testCreateVault2() {
			var vaultDto = new VaultResource.VaultDto("vaultX", "Vault 1","This is a testvault.", Timestamp.valueOf("2020-02-20 20:20:20"), null, "masterkey1", "iterations1", "salt1");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vaultX")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3 returns 201")
		public void testCreateVault3() {
			var vaultDto = new VaultResource.VaultDto("vault3", "My Vault","Test vault 3", Timestamp.valueOf("2112-12-21 21:12:21"), null, "masterkey3", "42", "NaCl");

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
			given().contentType(ContentType.TEXT).body("jwe9999")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device3")
					.then().statusCode(201);
		}

		@Test
		@DisplayName("PUT /vaults/vault2/keys/device1 returns 409 due to user access")
		public void testGrantAccess2() {
			given().contentType(ContentType.TEXT).body("jwe4")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3/keys/device4 returns 409 due to group access")
		public void testGrantAccess3() {
			given().contentType(ContentType.TEXT).body("jwe4")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault3", "device5")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault1/keys/nonExistingDevice returns 404")
		public void testGrantAccess4() {
			given().contentType(ContentType.TEXT).body("jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "nonExistingDevice")
					.then().statusCode(404);
		}

	}

	@Nested
	@DisplayName("Managing members as user2")
	@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"}, clean = false)
	@TestSecurity(user = "User Name 2", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageMembers {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/vault2/members/user9999 returns 404")
		public void addNonExistingUser() {
			when().put("/vaults/{vaultId}/members/{userId}", "vault2", "user9999")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("GET /vaults/vault2/access does not contain user2")
		public void getAccess1() {
			when().get("/vaults/{vaultId}/access", "vault2")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant does not contains device2")
		public void testGetDevicesRequiringAccess1() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(4)
		@DisplayName("PUT /vaults/vault2/members/user2 returns 201")
		public void addUser1() {
			when().put("/vaults/{vaultId}/members/{userId}", "vault2", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/vault2/access does contain user2")
		public void getMembers2() {
			when().get("/vaults/{vaultId}/access", "vault2")
					.then().statusCode(200)
					.body("users.id", hasItems("user2"));
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains device2")
		public void testGetDevicesRequiringAccess2() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device2"));
		}

		@Test
		@Order(7)
		@DisplayName("PUT /vaults/vault2/keys/device2 returns 201")
		public void testGrantAccess1() {
			given().contentType(ContentType.TEXT).body("jwe9999")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device2")
					.then().statusCode(201);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device2")
		public void testGetDevicesRequiringAccess3() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device2")));
		}

		@Test
		@Order(9)
		@DisplayName("PUT /devices/device9999 returns 201")
		public void testCreateDevice2() {
			var deviceDto = new DeviceResource.DeviceDto("device9999", "Computer 9999", "publickey9999", "user2", Set.of());

			given().contentType(ContentType.JSON).body(deviceDto)
					.when().put("/devices/{deviceId}", "device9999")
					.then().statusCode(201);
		}

		@Test
		@Order(10)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device9999")
		public void testGetDevicesRequiringAccess4() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device9999"));
		}

		@Test
		@Order(11)
		@DisplayName("DELETE /vaults/vault2/members/user2 returns 204")
		public void removeUser2() {
			when().delete("/vaults/{vaultId}/members/{userId}", "vault2", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/vault2/acces does not contain user2")
		public void getMembers3() {
			when().get("/vaults/{vaultId}/access", "vault2")
					.then().statusCode(200)
					.body("users.id", not(hasItems("user2")));
		}
	}

	@Nested
	@DisplayName("Managing groups as user2")
	@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), clean = false)
	@TestSecurity(user = "User Name 2", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class ManageGroups {

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/vault4/groups/group3000 returns 404")
		public void addNonExistingGroup() {
			when().put("/vaults/{vaultId}/groups/{groupId}", "vault4", "group3000")
					.then().statusCode(404);
		}

		@Test
		@Order(2)
		@DisplayName("GET /vaults/vault4/access does not contain group3")
		public void getAccess2() {
			when().get("/vaults/{vaultId}/access", "vault4")
					.then().statusCode(200)
					.body("groups.id", not(hasItems("group3")));
		}

		@Test
		@Order(3)
		@DisplayName("PUT /vaults/vault4/groups/group3 returns 201")
		public void addGroup1() {
			when().put("/vaults/{vaultId}/groups/{groupId}", "vault4", "group3")
					.then().statusCode(201);
		}

		@Test
		@Order(4)
		@DisplayName("GET /vaults/vault4/access does contain group3")
		public void getMembers4() {
			when().get("/vaults/{vaultId}/access", "vault4")
					.then().statusCode(200)
					.body("groups.id", hasItems("group3"));
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/vault4/devices-requiring-access-grant contains device5")
		public void testGetDevicesRequiringAccess3() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault4")
					.then().statusCode(200)
					.body("id", hasItems("device5"));
		}

		@Test
		@Order(6)
		@DisplayName("PUT /vaults/vault4/keys/device5 returns 201")
		public void testGrantAccess2() {
			given().contentType(ContentType.TEXT).body("jwe9999")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault4", "device5")
					.then().statusCode(201);
		}

		@Test
		@Order(7)
		@DisplayName("GET /vaults/vault4/devices-requiring-access-grant contains not device5")
		public void testGetDevicesRequiringAccess4() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault4")
					.then().statusCode(200)
					.body("id", not(hasItems("device5")));
		}

		@Test
		@Order(8)
		@DisplayName("DELETE /vaults/vault4/groups/group2 returns 204")
		public void removeGroup2() {
			when().delete("/vaults/{vaultId}/groups/{groupId}", "vault4", "group2")
					.then().statusCode(204);
		}

		@Test
		@Order(9)
		@DisplayName("GET /vaults/vault4/acces does not contain group2")
		public void getMembers5() {
			when().get("/vaults/{vaultId}/access", "vault4")
					.then().statusCode(200)
					.body("groups.id", not(hasItems("group2")));
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
				"GET, /vaults/vault1/access",
				"PUT, /vaults/vault1/members/user1",
				"DELETE, /vaults/vault1/members/user1",
				"GET, /vaults/vault1/devices-requiring-access-grant",
				"GET, /vaults/vault1/keys/device1"
		})
		public void testGet(String method, String path) {
			when().request(method, path)
					.then().statusCode(401);
		}

	}

}