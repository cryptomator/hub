package org.cryptomator.hub.entities;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;

@QuarkusTest
@DisplayName("Persistent Entities")
public class EntityIT {

	@Inject
	AccessTokenRepository accessTokenRepo;
	@Inject
	AgroalDataSource dataSource;

	@Test
	@TestTransaction
	@DisplayName("Removing a User cascades to Access")
	public void removingUserCascadesToAccess() throws SQLException {
		try (var c = dataSource.getConnection(); var s = c.createStatement()) {
			// test data will be removed via @TestTransaction
			s.execute("""
					INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
					INSERT INTO "user_details" ("id") VALUES ('user999');
					INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey") VALUES ('user999', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user999');
					""");
		}

		var deleted = User.deleteById("user999");
		var matchAfter = accessTokenRepo.findAll().stream().anyMatch(a -> "user999".equals(a.user.id));
		Assertions.assertTrue(deleted);
		Assertions.assertFalse(matchAfter);
	}

	@Test
	@TestTransaction
	@DisplayName("Retrieve the correct token when a user has access to multiple vaults")
	public void testGetCorrectTokenForDeviceWithAcessToMultipleVaults() {
		var token = accessTokenRepo.unlock(UUID.fromString("7E57C0DE-0000-4000-8000-000100001111"), "user1");
		Assertions.assertEquals(UUID.fromString("7E57C0DE-0000-4000-8000-000100001111"), token.vault.id);
		Assertions.assertEquals("user1", token.user.id);
		Assertions.assertEquals("jwe.jwe.jwe.vault1.user1", token.vaultKey);
	}
}