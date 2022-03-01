package org.cryptomator.hub.entities;

import com.radcortez.flyway.test.annotation.DataSource;
import com.radcortez.flyway.test.annotation.FlywayTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
			Assumptions.assumeTrue(vault.useraccess.stream().anyMatch(a -> "user1".equals(a.user.id)));

			vault.members.removeIf(m -> "user1".equals(m.id));
			vault.persist();
		});

		tx(() -> {
			User user = User.findById("user1");
			Assertions.assertNotNull(user);

			Vault vault = Vault.findById("vault1");
			Assertions.assertFalse(vault.useraccess.stream().anyMatch(a -> user.equals(a.user)));
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
			boolean match = Useraccess.<Useraccess>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertFalse(match);
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public static void tx(Runnable validation) {
		validation.run();
	}

}