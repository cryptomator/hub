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
			boolean match = Access.<Access>findAll().stream().anyMatch(a -> "device1".equals(a.device.id));
			Assertions.assertFalse(match);
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public static void tx(Runnable validation) {
		validation.run();
	}

}