package org.cryptomator.hub.license;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.EnumSet;

@ApplicationScoped
public class LicenseHolder {

	// TODO this is just a stub

	public boolean isExpired() {
		return false;
	}

	public Collection<LicenseClaim> getClaims() {
		return EnumSet.of(LicenseClaim.FOO);
	}

}
