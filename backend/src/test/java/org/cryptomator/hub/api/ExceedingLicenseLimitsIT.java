package org.cryptomator.hub.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@QuarkusTest
@TestSecurity(user = "User Name 1", roles = {"user", "create-vaults"})
@OidcSecurity(claims = {
		@Claim(key = "sub", value = "user1")
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExceedingLicenseLimitsIT {

	@Inject
	Group.Repository groupRepo;
	@Inject
	User.Repository userRepo;
	@Inject
	EffectiveGroupMembership.Repository effectiveGroupMembershipRepo;
	@Inject
	Vault.Repository vaultRepo;
	@Inject
	VaultAccess.Repository vaultAccessRepo;

	private final VaultResourceIT vaultResourceIT;

	public ExceedingLicenseLimitsIT(VaultResourceIT vaultResourceIT) {
		this.vaultResourceIT = vaultResourceIT;
	}

	@BeforeAll
	@Transactional
	void setupTestData() {
		var user91 = new User();
		user91.setId("user91");
		user91.setName("user name 91");

		var user92 = new User();
		user92.setId("user92");
		user92.setName("user name 92");

		var user93 = new User();
		user93.setId("user93");
		user93.setName("user name 93");

		var user94 = new User();
		user94.setId("user94");
		user94.setName("user name 94");

		var user95A = new User();
		user95A.setId("user95_A");
		user95A.setName("user name Archived");

		userRepo.persist(user91, user92, user93, user94, user95A);

		var group91 = new Group();
		group91.setId("group91");
		group91.setName("Group 91");
		group91.getMembers().add(user91);
		group91.getMembers().add(user92);
		group91.getMembers().add(user93);
		group91.getMembers().add(user94);
		groupRepo.persist(group91);

		effectiveGroupMembershipRepo.updateGroups(List.of("group91"));

		var access = new VaultAccess();
		access.setVault(vaultRepo.findById(UUID.fromString("7E57C0DE-0000-4000-8000-00010000AAAA")));
		access.setAuthority(user95A);
		access.setRole(VaultAccess.Role.MEMBER);
		vaultAccessRepo.persist(access);
	}

	@AfterAll
	@Transactional
	void cleanupTestData() {
		groupRepo.deleteById("group91");
		userRepo.deleteByIds(List.of("user91", "user92", "user93", "user94", "user95_A"));
	}

	@Test
	@Order(0)
	@DisplayName("POST /vaults/7E57C0DE-0000-4000-8000-000100001111/access-tokens returns 402 for [user91, user92, user93, user94]")
	void grantAccessExceedingSeats() {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() == 2);
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
	void addGroupToVaultExceedingSeats() {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() == 2);

		given().when().put("/vaults/{vaultId}/groups/{groupId}", "7E57C0DE-0000-4000-8000-000100001111", "group91")
				.then().statusCode(402);
	}

	@Order(2)
	@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/userXX returns 201")
	@ParameterizedTest(name = "Adding user {0} succeeds")
	@CsvSource(value = {"0,user91", "1,user92", "2,user93"})
	void addUserToVaultNotExceedingSeats(String run, String userId) {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() == 2 + Integer.parseInt(run));

		given().when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", userId)
				.then().statusCode(201);
	}

	@Test
	@Order(3)
	@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111/users/user94 returns 402")
	void addUserToVaultExceedingSeats() {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() == 5);

		given().when().put("/vaults/{vaultId}/users/{usersId}", "7E57C0DE-0000-4000-8000-000100001111", "user94")
				.then().statusCode(402);
	}

	@Test
	@Order(4)
	@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100001111 (as user1) returns 200 with only updated name, description and archive flag, despite exceeding license")
	void testUpdateVaultDespiteLicenseExceeded() {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() == 5);
		var vaultId = "7E57C0DE-0000-4000-8000-000100001111";

		var vaultDto = new VaultResource.VaultDto(UUID.fromString(vaultId), "Vault 1", "This is a testvault.", false, Instant.parse("2222-11-11T11:11:11Z"), "someValue", -1, "doNotUpdate", "doNotUpdate", "doNotUpdate");
		given().contentType(ContentType.JSON)
				.body(vaultDto)
				.when().put("/vaults/{vaultId}", vaultId)
				.then().statusCode(200)
				.body("id", equalToIgnoringCase(vaultId))
				.body("name", equalTo("Vault 1"))
				.body("description", equalTo("This is a testvault."))
				.body("archived", equalTo(false));
	}

	@Test
	@Order(5)
	@DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-0001FFFF3333 (as user1) exceeding the license returns 402")
	void testCreateVaultExceedingSeats() throws SQLException {
		try (var c = vaultResourceIT.dataSource.getConnection(); var s = c.createStatement()) {
			s.execute("""
					INSERT INTO "vault_access" ("vault_id", "authority_id")
					VALUES
						('7E57C0DE-0000-4000-8000-000100001111', 'group91');
					""");
		}

		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsers() > 5);

		var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-0001FFFF3333");
		var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", "Test vault 4", false, Instant.parse("2112-12-21T21:12:21Z"), "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");
		given().contentType(ContentType.JSON).body(vaultDto)
				.when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-0001FFFF3333")
				.then().statusCode(402);
	}

	@Test
	@Order(7)
	@DisplayName("unlock/legacyUnlock is granted, if (effective vault user) > license seats but (effective vault user with access token) <= license seat")
	void testUnlockAllowedExceedingLicenseSoftLimit() {
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsersWithAccessToken() <= 5);

		when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
				.then().statusCode(200);
		when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
				.then().statusCode(200)
				.body(is("legacy.jwe.jwe.vault2.device3"));
	}

	@Test
	@Order(8)
	@DisplayName("Unlock/legacyUnlock is blocked if (effective vault users with token) > license seats")
	void testUnlockBlockedExceedingLicenseHardLimit() throws SQLException {
		try (var c = vaultResourceIT.dataSource.getConnection(); var s = c.createStatement()) {
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
		Assumptions.assumeTrue(vaultResourceIT.effectiveVaultAccessRepo.countSeatOccupyingUsersWithAccessToken() > 5);

		when().get("/vaults/{vaultId}/access-token", "7E57C0DE-0000-4000-8000-000100001111")
				.then().statusCode(402);
		when().get("/vaults/{vaultId}/keys/{deviceId}", "7E57C0DE-0000-4000-8000-000100002222", "legacyDevice3")
				.then().statusCode(402);
	}

}
