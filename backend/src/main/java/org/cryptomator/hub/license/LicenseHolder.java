package org.cryptomator.hub.license;

import com.auth0.jwt.interfaces.DecodedJWT;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

@ApplicationScoped
public class LicenseHolder {
	private volatile DecodedJWT license;
	// TODO this is just a stub

	public LicenseHolder() {

	}

	public void set(DecodedJWT validatedToken) {
		Objects.requireNonNull(validatedToken);
		this.license = validatedToken;
	}

	public DecodedJWT get() {
		return license;
	}

	public boolean isExpired() {
		return license.getExpiresAt().toInstant().isBefore(Instant.now());
	}

	public Collection<LicenseClaim> getClaims() {
		return EnumSet.of(LicenseClaim.FOO);
	}

}
