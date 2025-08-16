package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cronutils.utils.Preconditions;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LicenseHolder {

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);

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

	@RestClient
	LicenseApi licenseApi;

	private DecodedJWT license;

	/**
	 * Makes sure a valid (but possibly expired) license exists.
	 * <p>
	 * Called during {@link org.cryptomator.hub.Main application startup}.
	 *
	 * @throws JWTVerificationException if the license is invalid
	 */
	@Transactional
	public void ensureLicenseExists() throws JWTVerificationException{
		var settings = settingsRepo.get();
		if (settings.getLicenseKey() != null && settings.getHubId() != null) {
			validateExistingLicense(settings);
		} else if (initialLicenseToken.isPresent() && initialId.isPresent()) {
			validateAndApplyInitLicense(settings, initialLicenseToken.get(), initialId.get());
		} else {
			requestAnonTrialLicense(settings);
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	void validateExistingLicense(Settings settings) throws JWTVerificationException {
		try {
			this.license = licenseValidator.validate(settings.getLicenseKey(), settings.getHubId());
			LOG.info("Verified existing license.");
		} catch (JWTVerificationException e) {
			LOG.warn("License in database is invalid or does not match hubId", e);
			LOG.warn("Deleting license entry. Please add the license over the REST API again.");
			throw e;
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	void validateAndApplyInitLicense(Settings settings, String initialLicenseToken, String initialHubId) throws JWTVerificationException {
		try {
			this.license = licenseValidator.validate(initialLicenseToken, initialHubId);
			settings.setLicenseKey(initialLicenseToken);
			settings.setHubId(initialHubId);
			settingsRepo.persistAndFlush(settings);
			LOG.info("Successfully imported license from property hub.initial-license.");
		} catch (JWTVerificationException e) {
			LOG.warn("Provided initial license is invalid or does not match inital hubId.", e);
			throw e;
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	void requestAnonTrialLicense(Settings settings) {
		LOG.info("No license found. Requesting trial license...");
		var hubId = UUID.randomUUID().toString();
		var challenge = licenseApi.generateTrialChallenge();
		var solution = solveChallenge(challenge);
		var trialLicense = licenseApi.verifyTrialChallenge(hubId, solution);
		this.license = licenseValidator.validate(trialLicense, hubId);
		settings.setLicenseKey(trialLicense);
		settings.setHubId(hubId);
		settingsRepo.persistAndFlush(settings);
		LOG.info("Successfully retrieved trial license.");
	}

	// visible for testing
	LicenseApi.Solution solveChallenge(LicenseApi.Challenge challenge) {
		HexFormat hex = HexFormat.of();
		MessageDigest sha256;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("Every implementation of the Java platform is required to support [...] SHA-256", e);
		}
		for (int i = 0; i < challenge.maxnumber(); i++) {
			var saltedSecret = challenge.salt() + i;
			sha256.update(saltedSecret.getBytes(StandardCharsets.US_ASCII));
			var attempt = hex.formatHex(sha256.digest());
			if (challenge.challenge().equals(attempt)) {
				return challenge.solve(i);
			}
		}
		throw new IllegalArgumentException("Unsolvable challenge");
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
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
	void refreshLicense() throws InterruptedException {
		if (get() != null) {
			randomMinuteSleeper.sleep(); // add random sleep between [0,59]min to reduce infrastructure load
			var refreshUrlClaim = get().getClaim("refreshUrl");
			if (refreshUrlClaim != null) {
				try {
					var refreshUrl = URI.create(refreshUrlClaim.asString());
					var refreshedLicense = requestLicenseRefresh(refreshUrl, get().getToken());
					set(refreshedLicense);
				} catch (LicenseRefreshFailedException e) {
					LOG.errorv("Failed to refresh license token. Request to {0} was answerd with response code {1,number,integer}", refreshUrlClaim, e.statusCode);
				} catch (IllegalArgumentException | IOException e) {
					LOG.error("Failed to refresh license token", e);
				} catch (JWTVerificationException e) {
					LOG.error("Failed to refresh license token. Refreshed token is invalid.", e);
				}
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

	@NotNull
	public DecodedJWT get() {
		return Preconditions.checkNotNull(license);
	}

	/**
	 * Checks if the license is set.
	 *
	 * @return {@code true}, if the license _is not null_. Otherwise false.
	 */
	@Deprecated // FIXME remove this method!
	public boolean isSet() {
		return license != null;
	}

	/**
	 * Checks if the license is expired.
	 *
	 * @return {@code true}, if the license expired, {@code false} otherwise.
	 */
	public boolean isExpired() {
		return Preconditions.checkNotNull(license).getExpiresAt().toInstant().isBefore(Instant.now());
	}

	/**
	 * Gets the number of seats in the license
	 *
	 * @return Number of seats of the license
	 */
	public long getSeats() {
		return Preconditions.checkNotNull(license).getClaim("seats").asLong();
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
