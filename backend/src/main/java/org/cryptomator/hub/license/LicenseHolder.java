package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class LicenseHolder {

	@Inject
	@ConfigProperty(name = "hub.managed-instance", defaultValue = "false")
	Boolean managedInstance;

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);
	private final LicenseValidator licenseValidator;
	private DecodedJWT license;

	LicenseHolder(LicenseValidator licenseValidator) {
		this.licenseValidator = licenseValidator;
	}

	/**
	 * Loads the license from the database, if present
	 */
	@PostConstruct
	void init() {
		var settings = Settings.get();
		if (settings.licenseKey != null) {
			try {
				this.license = licenseValidator.validate(settings.licenseKey, settings.hubId);
			} catch (JWTVerificationException e) {
				LOG.warn("License in database is invalid. Deleting entry. Please add the license over the REST API again.");
				settings.licenseKey = null;
				settings.persist();
			}
		}
	}

	/**
	 * Parses, verifies and persists the given token as the license in the database.
	 *
	 * @param token The string represenation of the JWT license
	 * @throws JWTVerificationException if the license cannot be verfied
	 */
	@Transactional
	public void set(String token) throws JWTVerificationException {
		Objects.requireNonNull(token);

		var settings = Settings.get();
		this.license = licenseValidator.validate(token, settings.hubId);
		settings.licenseKey = token;
		settings.persist();
	}

	public DecodedJWT get() {
		return license;
	}

	/**
	 * Checks if the license is set.
	 *
	 * @return {@code true}, if the license _is set_. Otherwise false.
	 */
	public boolean isSet() {
		return license != null;
	}

	/**
	 * Checks if the license is expired.
	 *
	 * @return {@code true}, if the license _is set and expired_. Otherwise false.
	 */
	public boolean isExpired() {
		return Optional.ofNullable(license) //
				.map(l -> l.getExpiresAt().toInstant().isBefore(Instant.now())) //
				.orElse(false);
	}

	/**
	 * Gets the number of available seats of the license
	 *
	 * @return Number of available seats, if license is not null. Otherwise {@value SelfHostedNoLicenseConstants#SEATS}.
	 */
	public long getAvailableSeats() {
		return Optional.ofNullable(license) //
				.map(l -> l.getClaim("seats")) //
				.map(Claim::asLong) //
				.orElseGet(this::getNoLicenseSeats);
	}

	public long getNoLicenseSeats() {
		if (!managedInstance) {
			return SelfHostedNoLicenseConstants.SEATS;
		} else {
			return ManagedInstanceNoLicenseConstants.SEATS;
		}
	}

	public boolean isManagedInstance() {
		return managedInstance;
	}

	public static class SelfHostedNoLicenseConstants {
		public static final long SEATS = 5;

		private SelfHostedNoLicenseConstants() {
			throw new IllegalStateException("Utility class");
		}
	}

	public static class ManagedInstanceNoLicenseConstants {
		public static final long SEATS = 0;

		private ManagedInstanceNoLicenseConstants() {
			throw new IllegalStateException("Utility class");
		}
	}

}
