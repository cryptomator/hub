package org.cryptomator.hub.entities;

import io.agroal.api.AgroalDataSource;
import io.quarkus.panache.common.Parameters;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@QuarkusTest
@DisplayName("Persistent Entities")
public class EntityIntegrationTest {

	@Inject
	AgroalDataSource dataSource;

	@Inject
	EntityManager entityManager;

	@Test
	@TestTransaction
	@DisplayName("Removing a User cascades to Access")
	public void removingUserCascadesToAccess() throws SQLException {
		try (var s = dataSource.getConnection().createStatement()) {
			// test data will be removed via @TestTransaction
			s.execute("""
					INSERT INTO "authority" ("id", "type", "name") VALUES ('user999', 'USER', 'User 999');
					INSERT INTO "user_details" ("id") VALUES ('user999');
					INSERT INTO "access_token" ("user_id", "vault_id", "vault_key_jwe") VALUES ('user999', '7E57C0DE-0000-4000-8000-000100001111', 'jwe4');
					""");
		}

		var deleted = User.deleteById("user999");
		var matchAfter = AccessToken.<AccessToken>findAll().stream().anyMatch(a -> "user999".equals(a.user.id));
		Assertions.assertTrue(deleted);
		Assertions.assertFalse(matchAfter);
	}

	@Test
	@TestTransaction
	@DisplayName("User's device names need to be unique")
	public void testAddNonUniqueDeviceName() {
		Device existingDevice = Device.findById("device1");
		Device conflictingDevice = new Device();
		conflictingDevice.id = "deviceX";
		conflictingDevice.name = existingDevice.name;
		conflictingDevice.owner = existingDevice.owner;
		conflictingDevice.publickey = "XYZ";
		conflictingDevice.creationTime = Instant.parse("2020-02-20T20:20:20Z");

		PersistenceException thrown = Assertions.assertThrows(PersistenceException.class, conflictingDevice::persistAndFlush);
		Assertions.assertInstanceOf(ConstraintViolationException.class, thrown);
	}

	@Test
	@TestTransaction
	@DisplayName("Retrieve the correct token when a user has access to multiple vaults")
	public void testGetCorrectTokenForDeviceWithAcessToMultipleVaults() {
		List<AccessToken> tokens = AccessToken
				.<AccessToken>find("#AccessToken.get", Parameters.with("userId", "user1")
						.and("vaultId", UUID.fromString("7E57C0DE-0000-4000-8000-000100001111")))
				.stream().toList();

		var token = tokens.get(0);

		Assertions.assertEquals(1, tokens.size());
		Assertions.assertEquals(UUID.fromString("7E57C0DE-0000-4000-8000-000100001111"), token.vault.id);
		Assertions.assertEquals("user1", token.user.id);
		Assertions.assertEquals("jwe.jwe.jwe.vault1.user1", token.jwe);
	}
}