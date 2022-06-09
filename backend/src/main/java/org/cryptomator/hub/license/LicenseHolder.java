package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.cryptomator.hub.entities.Billing;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class LicenseHolder {

	private final LicenseValidator licenseValidator;
	private volatile DecodedJWT license;

	LicenseHolder(LicenseValidator licenseValidator) {
		this.licenseValidator = licenseValidator;
		var billingEntry = Billing.<Billing>findAll().firstResult();
		if(billingEntry.token != null) {
			try {
				this.license = licenseValidator.validate(billingEntry.token, billingEntry.hubId);
			} catch (JWTVerificationException e) {
				//TODO: Log error
			}
		}
	}

	/**
	 * Parses, verifies and persists the given token as the license in the database.
	 *
	 * @param token The string represenation of the JWT license
	 * @throws JWTVerificationException if the license cannot be verfied
	 */
	public void set(String token) throws JWTVerificationException {
		Objects.requireNonNull(token);

		var billingEntry = Billing.<Billing>findAll().firstResult();
		this.license = licenseValidator.validate(token, billingEntry.hubId);
		billingEntry.token = token;
		billingEntry.persist();
	}

	public DecodedJWT get() {
		return license;
	}

	public boolean isExpired() {
		return Optional.ofNullable(license).map(l -> l.getExpiresAt().toInstant().isBefore(Instant.now())).orElse(false); //TODO: should be a non existing license be always expired?
	}

	public long getAvailableSeats() {
		return Optional.ofNullable(license) //
				.map(l -> { //
					var c = l.getClaim("seats"); //
					return c.isNull()? 0L : c.asLong(); //
				}) //
				.orElse(0L);
	}

}
