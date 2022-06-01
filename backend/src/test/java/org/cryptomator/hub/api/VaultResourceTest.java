package org.cryptomator.hub.api;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
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

import javax.inject.Inject;
import java.sql.SQLException;
import java.sql.Timestamp;
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

	@Inject
	AgroalDataSource dataSource;

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
		@DisplayName("GET /vaults/vault1/members returns 403")
		public void testGetAccess() {
			when().get("/vaults/{vaultId}/members", "vault1")
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
	@DisplayName("As vault owner user1")
	@TestSecurity(user = "User Name 1", roles = {"user", "vault-owner"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	public class AsVaultOwner {

		@Test
		@DisplayName("GET /vaults/vault1/members returns 200")
		public void testGetAccess1() {
			when().get("/vaults/{vaultId}/members", "vault1")
					.then().statusCode(200)
					.body("id", hasItems("user1", "user2"))
					.and().body("type", not(hasItems("GROUP")));
		}

		@Test
		@DisplayName("GET /vaults/vault2/members returns 403")
		public void testGetAccess2() {
			when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("PUT /vaults/vault1 returns 409")
		public void testCreateVault1() {
			var vaultDto = new VaultResource.VaultDto("vault1", "My Vault", "Test vault 1", Timestamp.valueOf("1999-11-19 19:19:19"), null, "masterkey3", "42", "NaCl");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vaultX returns 409")
		public void testCreateVault2() {
			var vaultDto = new VaultResource.VaultDto("vaultX", "Vault 1", "This is a testvault.", Timestamp.valueOf("2020-02-20 20:20:20"), null, "masterkey1", "iterations1", "salt1");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vaultX")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3 returns 201")
		public void testCreateVault3() {
			var vaultDto = new VaultResource.VaultDto("vault3", "My Vault", "Test vault 3", Timestamp.valueOf("2112-12-21 21:12:21"), null, "masterkey3", "42", "NaCl");

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
		@DisplayName("PUT /vaults/vault1/keys/device1 returns 409 due to user access already granted")
		public void testGrantAccess2() {
			given().contentType(ContentType.TEXT).body("jwe1")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault1", "device1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault2/keys/device3 returns 409 due to group access already granted")
		@TestSecurity(user = "User Name 2", roles = {"user", "vault-owner"}) //we switch here for easy usage
		@OidcSecurity(claims = {
				@Claim(key = "sub", value = "user2")
		})
		public void testGrantAccess3() throws SQLException {
			given().contentType(ContentType.TEXT).body("jwe3")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device3")
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
		@DisplayName("GET /vaults/vault2/members does not contain user2")
		public void getAccess1() {
			when().get("/vaults/{vaultId}/members", "vault2")
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
			when().put("/vaults/{vaultId}/users/{userId}", "vault2", "user2")
					.then().statusCode(201);
		}

		@Test
		@Order(5)
		@DisplayName("GET /vaults/vault2/members does contain user2")
		public void getMembers2() {
			when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("user2"));
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
			when().delete("/vaults/{vaultId}/users/{userId}", "vault2", "user2")
					.then().statusCode(204);
		}

		@Test
		@Order(12)
		@DisplayName("GET /vaults/vault2/acces does not contain user2")
		public void getMembers3() {
			when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("user2")));
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
		@DisplayName("PUT /vaults/vault2/groups/group11 returns 201")
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

			when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group91")
					.then().statusCode(201);
		}

		@Test
		@Order(3)
		@DisplayName("GET /vaults/vault2/members does contain group91")
		public void getMembers4() {
			when().get("/vaults/{vaultId}/members", "vault2")
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
							
						INSERT INTO "device" ("id", "owner_id", "name", "publickey")
						VALUES
							('device93', 'user90', 'Computer 9', 'publickey90');
						""");
			}

			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", hasItems("device93"));
		}

		@Test
		@Order(5)
		@DisplayName("PUT /vaults/vault2/keys/device93 returns 201")
		public void testGrantAccess2() {
			given().contentType(ContentType.TEXT).body("jwe99")
					.when().put("/vaults/{vaultId}/keys/{deviceId}", "vault2", "device93")
					.then().statusCode(201);
		}

		@Test
		@Order(6)
		@DisplayName("GET /vaults/vault2/devices-requiring-access-grant contains not device93")
		public void testGetDevicesRequiringAccess4() {
			when().get("/vaults/{vaultId}/devices-requiring-access-grant", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("device93")));
		}

		@Test
		@Order(7)
		@DisplayName("DELETE /vaults/vault2/groups/group91 returns 204")
		public void removeGroup2() {
			when().delete("/vaults/{vaultId}/groups/{groupId}", "vault2", "group91")
					.then().statusCode(204);
		}

		@Test
		@Order(8)
		@DisplayName("GET /vaults/vault2/members does not contain group91")
		public void getMembers5() {
			when().get("/vaults/{vaultId}/members", "vault2")
					.then().statusCode(200)
					.body("id", not(hasItems("group91")));
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