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
			Assumptions.assumeTrue(vault.userAccess.stream().anyMatch(a -> "user1".equals(a.user.id)));

			vault.members.removeIf(m -> "user1".equals(m.id));
			vault.persist();
		});

		tx(() -> {
			User user = User.findById("user1");
			Assertions.assertNotNull(user);

			Vault vault = Vault.findById("vault1");
			Assertions.assertFalse(vault.userAccess.stream().anyMatch(a -> user.equals(a.user)));
		});
	}

	@Test
	@DisplayName("Removing a Vault Group cascades to Access")
	public void removeGroupCascadesToGroupAccess() {
		tx(() -> {
			Vault vault = Vault.findById("vault3");
			Assumptions.assumeTrue(vault.groupAccess.stream().anyMatch(ga -> "group1".equals(ga.group.id)));

			vault.groups.removeIf(g -> "group1".equals(g.id));
			vault.persist();
		});

		tx(() -> {
			Group group = Group.findById("group1");
			Assertions.assertNotNull(group);

			Vault vault = Vault.findById("vault3");
			Assertions.assertFalse(vault.groupAccess.stream().anyMatch(ga -> group.equals(ga.group)));
		});
	}

	@Test
	@DisplayName("Removing a Device cascades to Access")
	public void removingDeviceCascadesToAccess() {
		tx(() -> {
			Device device = Device.findById("device1");
			/* FIXME Affects the deletion of the device such that the following transaction fails but should be executed.
			boolean userMatch = UserAccess.<UserAccess>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			boolean groupMatch = GroupAccess.<GroupAccess>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertTrue(userMatch);
			Assertions.assertTrue(groupMatch);
			*/
			device.delete();
		});

		tx(() -> {
			boolean userMatch = UserAccess.<UserAccess>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			boolean groupMatch = GroupAccess.<GroupAccess>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertFalse(userMatch);
			Assertions.assertFalse(groupMatch);
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public static void tx(Runnable validation) {
		validation.run();
	}

}