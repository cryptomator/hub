package org.cryptomator.hub.entities;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.panache.common.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"), additionalLocations = {"classpath:org/cryptomator/hub/flyway"})
@DisplayName("Persistent Entities")
public class EntityIntegrationTest {

	@Inject
	AgroalDataSource dataSource;

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public static void tx(Runnable validation) {
		validation.run();
	}

	@Test
	@DisplayName("Removing a Device cascades to Access")
	public void removingDeviceCascadesToAccess() {
		tx(() -> {
			Device device = Device.findById("device1");
			/* FIXME Affects the deletion of the device such that the following transaction fails but should be executed.
			boolean match = Access.<Access>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertTrue(match);
			*/
			device.delete();
		});

		tx(() -> {
			var match = AccessToken.<AccessToken>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertFalse(match);
		});
	}

	@Test
	@DisplayName("User's device names need to be unique")
	public void testAddNonUniqueDeviceName() {
		tx(() -> {
			Device existingDevice = Device.findById("device1");
			Device conflictingDevice = new Device();
			conflictingDevice.id = "deviceX";
			conflictingDevice.name = existingDevice.name;
			conflictingDevice.owner = existingDevice.owner;
			conflictingDevice.publickey = "XYZ";
			conflictingDevice.creationTime = Timestamp.valueOf("2020-02-20 20:20:20");

			PersistenceException thrown = Assertions.assertThrows(PersistenceException.class, conflictingDevice::persistAndFlush);
			Assertions.assertInstanceOf(ConstraintViolationException.class, thrown.getCause());
		});
	}

	@Test
	@DisplayName("Retrieve the correct token when a device has access to multiple vaults")
	public void testGetCorrectTokenForDeviceWithAcessToMultipleVaults() throws SQLException {
		try (var s = dataSource.getConnection().createStatement()) {
			s.execute("""
					INSERT INTO "access_token" ("device_id", "vault_id", "jwe") VALUES ('device3', 'vault1', 'jwe4');
					""");

			List<AccessToken> tokens = AccessToken
					.<AccessToken>find("#AccessToken.get", Parameters.with("deviceId", "device3")
							.and("vaultId", "vault1")
							.and("userId", "user1"))
					.stream().toList();

			var token = tokens.get(0);

			Assertions.assertEquals(1, tokens.size());
			Assertions.assertEquals("vault1", token.vault.id);
			Assertions.assertEquals("device3", token.device.id);
			Assertions.assertEquals("jwe4", token.jwe);
		}
	}
}