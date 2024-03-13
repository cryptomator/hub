package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class LicenseHolder {

	@Inject
	@ConfigProperty(name = "hub.managed-instance", defaultValue = "false")
	Boolean managedInstance;

	@Inject
	@ConfigProperty(name = "hub.initial-id")
	Optional<String> initialId;

	@Inject
	@ConfigProperty(name = "hub.initial-license")
	Optional<String> initialLicense;

	@Inject
	LicenseValidator licenseValidator;

	@Inject
	RandomMinuteSleeper randomMinuteSleeper;

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);
	private DecodedJWT license;

	/**
	 * Loads the license from the database or from init props, if present
	 */
	@PostConstruct
	void init() {
		var settings = Settings.get();
		if (settings.licenseKey != null) {
			validateLicense(settings.licenseKey, settings.hubId);
		} else if (initialId.isPresent() && initialLicense.isPresent()) {
			applyInitialHubIdAndLicense(initialId.get(), initialLicense.get());
		}
	}

	@Transactional
	void validateLicense(String licenseKey, String hubId) {
		try {
			this.license = licenseValidator.validate(licenseKey, hubId);
		} catch (JWTVerificationException e) {
			LOG.warn("Provided license is invalid. Deleting entry. Please add the license over the REST API again.");
			var settings = Settings.get();
			settings.licenseKey = null;
			settings.persistAndFlush();
		}
	}

	@Transactional
	void applyInitialHubIdAndLicense(String initialId, String initialLicense) {
		try {
			this.license = licenseValidator.validate(initialLicense, initialId);
			var settings = Settings.get();
			settings.licenseKey = initialLicense;
			settings.hubId = initialId;
			settings.persistAndFlush();
		} catch (JWTVerificationException e) {
			LOG.warn("Provided initial license is invalid.");
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
		settings.persistAndFlush();
	}

	//used for testing
	public void reloadLicense() throws JWTVerificationException {
		var settings = Settings.get();
		this.license = licenseValidator.validate(settings.licenseKey, settings.hubId);
	}

	/**
	 * Attempts to refresh the Hub licence every day between 01:00:00 and 02:00:00 AM UTC if claim refreshURL is present.
	 */
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
	void refreshLicenseScheduler() throws InterruptedException {
		if (license != null) {
			randomMinuteSleeper.sleep(); // add random sleep between [0,59]min to reduce infrastructure load
			var refreshUrl = licenseValidator.refreshUrl(license.getToken());
			if (refreshUrl.isPresent()) {
				var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
				refreshLicense(refreshUrl.get(), license.getToken(), client);
			}
		}
	}

	//visible for testing
	void refreshLicense(String refreshUrl, String license, HttpClient client) throws InterruptedException {
		var parameters = Map.of("token", license);
		var body = parameters.entrySet() //
				.stream() //
				.map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)) //
				.collect(Collectors.joining("&"));
		var request = HttpRequest.newBuilder() //
				.uri(URI.create(refreshUrl)) //
				.headers("Content-Type", "application/x-www-form-urlencoded") //
				.POST(HttpRequest.BodyPublishers.ofString(body)) //
				.version(HttpClient.Version.HTTP_1_1) //
				.build();
		try {
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200 && !response.body().isEmpty()) {
				set(response.body());
			} else {
				LOG.error("Failed to refresh license token with response code: " + response.statusCode());
			}
		} catch (IOException | JWTVerificationException e) {
			LOG.error("Failed to refresh license token", e);
		}
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
