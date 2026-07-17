package org.cryptomator.hub.license;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class UnconfiguredLicenseTest {

	@Test
	@DisplayName("Placeholder license carries the given hub ID, 0 seats and expires in the year 3000")
	void testCreate() {
		var jwt = UnconfiguredLicense.create("42");

		Assertions.assertEquals("42", jwt.getId());
		Assertions.assertEquals(0L, jwt.getClaim("seats").asLong());
		Assertions.assertEquals(Instant.parse("3000-01-01T00:00:00Z"), jwt.getExpiresAtAsInstant());
		Assertions.assertEquals(Instant.EPOCH, jwt.getIssuedAtAsInstant());
	}

	@Test
	@DisplayName("Placeholder license contains an entitlements claim matching HubLicenseEntitlements.create()")
	void testEntitlementsClaim() {
		var jwt = UnconfiguredLicense.create("42");

		var entitlements = jwt.getClaim("org.cryptomator.hub.entitlements").as(HubLicenseEntitlements.class);

		Assertions.assertEquals(HubLicenseEntitlements.create(), entitlements);
	}

}
