package org.cryptomator.hub.entities;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

@QuarkusTest
@FlywayTest(value = @DataSource(url = "jdbc:h2:mem:test"))
@DisplayName("Persistent Entities")
public class EntityIntegrationTest {

	@Test
	@DisplayName("Removing a Vault Member cascades to Access")
	public void removeMemberCascadesToAccess() {
		tx(() -> {
			Vault vault = Vault.findById("vault1");
			Assumptions.assumeTrue(vault.access.stream().anyMatch(a -> "user1".equals(a.user.id)));

			vault.members.removeIf(m -> "user1".equals(m.id));
			vault.persist();
		});

		tx(() -> {
			User user = User.findById("user1");
			Assertions.assertNotNull(user);

			Vault vault = Vault.findById("vault1");
			Assertions.assertFalse(vault.access.stream().anyMatch(a -> user.equals(a.user)));
		});
	}

	@Test
	@DisplayName("Removing a Device cascades to Access")
	public void removingDeviceCascadesToAccess() {
		tx(() -> {
			Device device = Device.findById("device1");
			device.delete();
		});

		tx(() -> {
			boolean match = Access.<Access>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
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

			Device.persist(conflictingDevice);

			PersistenceException thrown = Assertions.assertThrows(PersistenceException.class, Device::flush);
			Assertions.assertInstanceOf(ConstraintViolationException.class, thrown.getCause());
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public static void tx(Runnable validation) {
		validation.run();
	}

}