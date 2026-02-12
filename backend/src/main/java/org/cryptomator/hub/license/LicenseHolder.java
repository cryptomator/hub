package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
	RandomSleeper randomSleeper;

	@Inject
	Settings.Repository settingsRepo;

	@RestClient
	LicenseApi licenseApi;

	private CompletableFuture<DecodedJWT> license;

	@PostConstruct
	void init() {
		this.license = CompletableFuture.supplyAsync(this::ensureLicenseExists);
	}

	/**
	 * Makes sure a valid (but possibly expired) license exists.
	 * <p>
	 * Called during {@link org.cryptomator.hub.Main application startup}.
	 *
	 * @throws JWTVerificationException if the license is invalid
	 */
	@Transactional
	DecodedJWT ensureLicenseExists() throws JWTVerificationException, WebApplicationException {
		var settings = settingsRepo.get();
		if (settings.getLicenseKey() != null && settings.getHubId() != null) {
			return validateExistingLicense(settings);
		} else if (initialLicenseToken.isPresent() && initialId.isPresent()) {
			return validateAndApplyInitLicense(settings, initialLicenseToken.get(), initialId.get());
		} else {
			return requestAnonTrialLicense(settings);
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	DecodedJWT validateExistingLicense(Settings settings) throws JWTVerificationException {
		try {
			var validated = licenseValidator.validate(settings.getLicenseKey(), settings.getHubId());
			LOG.info("Verified existing license.");
			return validated;
		} catch (JWTVerificationException e) {
			LOG.warn("License in database is invalid or does not match hubId", e);
			throw e;
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	DecodedJWT validateAndApplyInitLicense(Settings settings, String initialLicenseToken, String initialHubId) throws JWTVerificationException {
		try {
			var validated = licenseValidator.validate(initialLicenseToken, initialHubId);
			settings.setLicenseKey(initialLicenseToken);
			settings.setHubId(initialHubId);
			settingsRepo.persistAndFlush(settings);
			LOG.info("Successfully imported license from property hub.initial-license.");
			return validated;
		} catch (JWTVerificationException e) {
			LOG.warn("Provided initial license is invalid or does not match initial hubId.", e);
			throw e;
		}
	}

	@Transactional(Transactional.TxType.MANDATORY)
	DecodedJWT requestAnonTrialLicense(Settings settings) throws WebApplicationException {
		LOG.info("No license found. Requesting trial license...");
		var challenge = licenseApi.generateTrialChallenge();
		var solution = solveChallenge(challenge);
		var trialResponse = licenseApi.generateTrialLicense(solution.toCaptcha());
		var validated = licenseValidator.validate(trialResponse.licenseKey(), trialResponse.hubId());
		settings.setLicenseKey(trialResponse.licenseKey());
		settings.setHubId(trialResponse.hubId());
		settingsRepo.persistAndFlush(settings);
		LOG.info("Successfully retrieved trial license.");
		return validated;
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
		long start = System.nanoTime();
		for (int i = 0; i < challenge.maxnumber(); i++) {
			var saltedSecret = challenge.salt() + i;
			sha256.update(saltedSecret.getBytes(StandardCharsets.US_ASCII));
			var attempt = hex.formatHex(sha256.digest());
			if (challenge.challenge().equals(attempt)) {
				long took = System.nanoTime() - start;
				return challenge.solve(i, took / 1_000_000);
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
		var newValidLicense = licenseValidator.validate(token, settings.getHubId());
		this.license = CompletableFuture.completedFuture(newValidLicense);
		settings.setLicenseKey(token);
		settingsRepo.persistAndFlush(settings);
	}

	/**
	 * Attempts to refresh the Hub licence every day between 01:00:00 and 02:00:00 AM UTC if claim refreshURL is present.
	 */
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
	@RunOnVirtualThread
	void refreshLicense() {
		var refreshUrlClaim = get().getClaim("refreshUrl");
		if (refreshUrlClaim.isMissing()) {
			LOG.error("Missing refreshUrl claim.");
			return;
		}
		try {
			randomSleeper.sleep(0, 59, ChronoUnit.MINUTES); // add random sleep to reduce infrastructure load
			var refreshUrl = URI.create(refreshUrlClaim.asString());
			var refreshedLicense = requestLicenseRefresh(refreshUrl, get().getToken());
			set(refreshedLicense);
		} catch (LicenseRefreshFailedException e) {
			LOG.errorv("Failed to refresh license token. Request to {0} was answered with response code {1,number,integer}", refreshUrlClaim, e.statusCode);
		} catch (IllegalArgumentException | IOException e) {
			LOG.error("Failed to refresh license token", e);
		} catch (JWTVerificationException e) {
			LOG.error("Failed to refresh license token. Refreshed token is invalid.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn("License refresh was interrupted", e);
		}
	}

	//visible for testing
	String requestLicenseRefresh(URI refreshUrl, String licenseToken) throws InterruptedException, IOException, LicenseRefreshFailedException {
		try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
			var body = "token=" + URLEncoder.encode(licenseToken, StandardCharsets.UTF_8);
			var request = HttpRequest.newBuilder() //
					.uri(refreshUrl) //
					.header("Content-Type", "application/x-www-form-urlencoded") //
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
		if (license == null) {
			throw new IllegalStateException();
		}
		try {
			return license.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Getting license interrupted", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to get license", e);
		}
	}

	public HubLicenseEntitlements getEntitlements() {
		var license = get();
		var entitlements = license.getClaim("org.cryptomator.hub.entitlements").as(HubLicenseEntitlements.class);
		// TODO: eventually "entitlements" claim will be mandatory and this fallback can be removed, see https://github.com/cryptomator/hub/issues/391
		if (entitlements == null) { // legacy (pre 1.5.0) license without "org.cryptomator.hub.entitlements" claim:
			return HubLicenseEntitlements.create().withSeats(license.getClaim("seats").asLong());
		} else {
			return entitlements;
		}
	}

	/**
	 * Checks if the license is expired.
	 *
	 * @return {@code true}, if the license expired, {@code false} otherwise.
	 */
	public boolean isExpired() {
		var license = get();
		if (license == null) {
			throw new IllegalStateException();
		}
		return license.getExpiresAt().toInstant().isBefore(Instant.now());
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
