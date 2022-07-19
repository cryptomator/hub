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
			when().put("/vaults/{vaultId}/users/{usersId}", "vault1", "newUser94")
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
			when().put("/vaults/{vaultId}/users/{usersId}", "vault2", "newUser91")
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
			when().put("/vaults/{vaultId}/users/{usersId}", "vault1", "user1")
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
			when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group1")
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
	@TestSecurity(user = "User Name 1", roles = {"user"})
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
			var vaultDto = new VaultResource.VaultDto("vault1", "My Vault", "Test vault 1", Timestamp.valueOf("1999-11-19 19:19:19"), "masterkey3", "42", "NaCl", "authPubKey3", "authPrvKey3");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vault1")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vaultX returns 409")
		public void testCreateVault2() {
			var vaultDto = new VaultResource.VaultDto("vaultX", "Vault 1", "This is a testvault.", Timestamp.valueOf("2020-02-20 20:20:20"), "masterkey1", "iterations1", "salt1", "authPubKey1", "authPrvKey1");

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().put("/vaults/{vaultId}", "vaultX")
					.then().statusCode(409);
		}

		@Test
		@DisplayName("PUT /vaults/vault3 returns 201")
		public void testCreateVault3() {
			var vaultDto = new VaultResource.VaultDto("vault3", "My Vault", "Test vault 3", Timestamp.valueOf("2112-12-21 21:12:21"), "masterkey3", "42", "NaCl", "authPubKey3", "authPrvKey3");

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
		@TestSecurity(user = "User Name 2", roles = {"user"}) //we switch here for easy usage
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
	@TestSecurity(user = "User Name 2", roles = {"user"})
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
			var deviceDto = new DeviceResource.DeviceDto("device9999", "Computer 9999", "publickey9999", "user2", Set.of(), Timestamp.valueOf("2020-02-20 20:20:20"));

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
		@DisplayName("GET /vaults/vault2/access does not contain user2")
		public void getMembers3() {
			when().get("/vaults/{vaultId}/members", "vault2")
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

		@Test
		@Order(1)
		@DisplayName("PUT /vaults/vault4/groups/group3000 returns 404")
		public void addNonExistingGroup() {
			when().put("/vaults/{vaultId}/groups/{groupId}", "vault4", "group3000")
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
							
						INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time")
						VALUES
							('device93', 'user90', 'Computer 9', 'publickey90', '2020-02-20 20:20:20');
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

			when().put("/vaults/{vaultId}/groups/{groupId}", "vault2", "group94")
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