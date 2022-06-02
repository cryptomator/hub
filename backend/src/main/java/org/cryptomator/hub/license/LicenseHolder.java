package org.cryptomator.hub.license;

import com.auth0.jwt.interfaces.DecodedJWT;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class LicenseHolder {
	private volatile DecodedJWT license;

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
		return Optional.ofNullable(license).map(l -> l.getExpiresAt().toInstant().isBefore(Instant.now())).orElse(false); //TODO: should be a non existing license be always expired?
	}

	public long getSeats() {
		return Optional.ofNullable(license) //
				.map(l -> { //
					var c = l.getClaim("seats"); //
					return c.isNull()? 0L : c.asLong(); //
				}) //
				.orElse(0L);
	}

	public Collection<LicenseClaim> getClaims() {
		return EnumSet.of(LicenseClaim.FOO);
	}

}
