package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
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
import java.util.Optional;

@ApplicationScoped
public class LicenseHolder implements Scheduled.SkipPredicate {

	private static final int SELFHOSTED_NOLICENSE_SEATS = 5;
	private static final int MANAGED_NOLICENSE_SEATS = 0;

	@Inject
	@ConfigProperty(name = "hub.managed-instance", defaultValue = "false")
	Boolean managedInstance;

	@Inject
	@ConfigProperty(name = "hub.initial-id")
	Optional<String> initialId;

	@Inject
	@ConfigProperty(name = "hub.initial-license")
	Optional<String> initialLicenseToken;

	@Inject
	LicenseValidator licenseValidator;

	@Inject
	RandomMinuteSleeper randomMinuteSleeper;
	@Inject
	Settings.Repository settingsRepo;

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);
	private DecodedJWT license;

	/**
	 * Loads the license from the database or from init props, if present
	 */
	@PostConstruct
	void init() {
		var settings = settingsRepo.get();
		if (settings.getLicenseKey() != null && settings.getHubId() != null) {
			validateOrResetExistingLicense(settings);
		} else if (initialLicenseToken.isPresent() && initialId.isPresent()) {
			validateAndApplyInitLicense(settings, initialLicenseToken.get(), initialId.get());
		}
	}

	@Transactional
	void validateOrResetExistingLicense(Settings settings) {
		try {
			this.license = licenseValidator.validate(settings.getLicenseKey(), settings.getHubId());
		} catch (JWTVerificationException e) {
			LOG.warn("License in database is invalid or does not match hubId", e);
			LOG.warn("Deleting license entry. Please add the license over the REST API again.");
			settings.setLicenseKey(null);
			settingsRepo.persistAndFlush(settings);
		}
	}

	@Transactional
	void validateAndApplyInitLicense(Settings settings, String initialLicenseToken, String initialHubId) {
		try {
			this.license = licenseValidator.validate(initialLicenseToken, initialHubId);
			settings.setLicenseKey(initialLicenseToken);
			settings.setHubId(initialHubId);
			settingsRepo.persistAndFlush(settings);
		} catch (JWTVerificationException e) {
			LOG.warn("Provided initial license is invalid or does not match inital hubId.", e);
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
		var settings = settingsRepo.get();
		this.license = licenseValidator.validate(token, settings.getHubId());
		settings.setLicenseKey(token);
		settingsRepo.persistAndFlush(settings);
	}

	/**
	 * Attempts to refresh the Hub licence every day between 01:00:00 and 02:00:00 AM UTC if claim refreshURL is present.
	 */
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, skipExecutionIf = LicenseHolder.class)
	void refreshLicense() throws InterruptedException {
		randomMinuteSleeper.sleep(); // add random sleep between [0,59]min to reduce infrastructure load
		var refreshUrlClaim = get().getClaim("refreshUrl");
		if (refreshUrlClaim != null) {
			try {
				var refreshUrl = URI.create(refreshUrlClaim.asString());
				var refreshedLicense = requestLicenseRefresh(refreshUrl, get().getToken());
				set(refreshedLicense);
			} catch (LicenseRefreshFailedException lrfe) {
				LOG.errorv("Failed to refresh license token. Request to {0} was answerd with response code {1,number,integer}", refreshUrlClaim, lrfe.statusCode);
			} catch (IllegalArgumentException | IOException e) {
				LOG.error("Failed to refresh license token", e);
			} catch (JWTVerificationException jve) {
				LOG.error("Failed to refresh license token. Refreshed token is invalid.", jve);
			}
		}
	}

	//visible for testing
	String requestLicenseRefresh(URI refreshUrl, String licenseToken) throws InterruptedException, IOException, LicenseRefreshFailedException {
		try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
			var body = "token=" + URLEncoder.encode(licenseToken, StandardCharsets.UTF_8);
			var request = HttpRequest.newBuilder() //
					.uri(refreshUrl) //
					.headers("Content-Type", "application/x-www-form-urlencoded") //
					.POST(HttpRequest.BodyPublishers.ofString(body)) //
					.version(HttpClient.Version.HTTP_1_1) //
					.build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200 && !response.body().isEmpty()) {
				return response.body();
			} else {
				throw new LicenseRefreshFailedException(response.statusCode(), body);
			}
		}
	}

	//necessary for skipExecutionIf
	@Override
	public boolean test(ScheduledExecution execution) {
		return license == null;
	}

	public DecodedJWT get() {
		return license;
	}

	/**
	 * Checks if the license is set.
	 *
	 * @return {@code true}, if the license _is not null_. Otherwise false.
	 */
	public boolean isSet() {
		return license != null;
	}

	/**
	 * Checks if the license is expired.
	 *
	 * @return {@code true}, if the license _is not nul and expired_. Otherwise false.
	 */
	public boolean isExpired() {
		return Optional.ofNullable(license) //
				.map(l -> l.getExpiresAt().toInstant().isBefore(Instant.now())) //
				.orElse(false);
	}

	/**
	 * Gets the number of seats in the license
	 *
	 * @return Number of seats of the license, if license is not null. Otherwise {@value SELFHOSTED_NOLICENSE_SEATS}.
	 */
	public long getSeats() {
		return Optional.ofNullable(license) //
				.map(l -> l.getClaim("seats")) //
				.map(Claim::asLong) //
				.orElseGet(this::getNoLicenseSeats);
	}

	private long getNoLicenseSeats() {
		if (!managedInstance) {
			return SELFHOSTED_NOLICENSE_SEATS;
		} else {
			return MANAGED_NOLICENSE_SEATS;
		}
	}

	public boolean isManagedInstance() {
		return managedInstance;
	}

	static class LicenseRefreshFailedException extends RuntimeException {
		final int statusCode;
		final String body;

		LicenseRefreshFailedException(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}
	}
}
