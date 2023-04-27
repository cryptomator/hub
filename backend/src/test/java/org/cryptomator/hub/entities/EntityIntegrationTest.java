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
	@DisplayName("Removing a Device cascades to Access")
	public void removingDeviceCascadesToAccess() throws SQLException {
		try (var s = dataSource.getConnection().createStatement()) {
			// test data will be removed via @TestTransaction
			s.execute("""
					INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time")
						VALUES ('device999', 'user1', 'Computer 999', 'publickey999', '2020-02-20 20:20:20');
					INSERT INTO "access_token" ("device_id", "vault_id", "jwe") VALUES ('device999', '7E57C0DE-0000-4000-8000-000100001111', 'jwe4');
					""");
		}

		var deleted = Device.deleteById("device999");
		var matchAfter = AccessToken.<AccessToken>findAll().stream().anyMatch(a -> "device999".equals(a.device.id));
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
	@DisplayName("Retrieve the correct token when a device has access to multiple vaults")
	public void testGetCorrectTokenForDeviceWithAcessToMultipleVaults() throws SQLException {
		try (var s = dataSource.getConnection().createStatement()) {
			// test data will be removed via @TestTransaction
			s.execute("""
					INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time")
						VALUES ('device999', 'user1', 'Computer 999', 'publickey999', '2020-02-20 20:20:20');
					INSERT INTO "access_token" ("device_id", "vault_id", "jwe") VALUES ('device999', '7E57C0DE-0000-4000-8000-000100001111', 'jwe4');
					INSERT INTO "access_token" ("device_id", "vault_id", "jwe") VALUES ('device999', '7E57C0DE-0000-4000-8000-000100002222', 'jwe5');
					""");
		}

		List<AccessToken> tokens = AccessToken
				.<AccessToken>find("#AccessToken.get", Parameters.with("deviceId", "device999")
						.and("vaultId", UUID.fromString("7E57C0DE-0000-4000-8000-000100001111"))
						.and("userId", "user1"))
				.stream().toList();

		var token = tokens.get(0);

		Assertions.assertEquals(1, tokens.size());
		Assertions.assertEquals(UUID.fromString("7E57C0DE-0000-4000-8000-000100001111"), token.vault.id);
		Assertions.assertEquals("device999", token.device.id);
		Assertions.assertEquals("jwe4", token.jwe);
	}
}