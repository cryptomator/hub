package org.cryptomator.hub.license;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

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
	@ConfigProperty(name = "hub.managed-api-username")
	Optional<String> managedApiUsername;

	@Inject
	@ConfigProperty(name = "hub.managed-api-password")
	Optional<String> managedApiPassword;

	@Inject
	LicenseValidator licenseValidator;

	@Inject
	RandomSleeper randomSleeper;

	@Inject
	Settings.Repository settingsRepo;

	@RestClient
	LicenseApi licenseApi;

	private DecodedJWT license;

	@PostConstruct
	void init() {
		this.license = this.ensureLicenseExists();
		// refresh upon startup:
		// except for trial licenses and recently issued licenses (to avoid restart-loop spam)
		var hasNotBeenIssuedRecently = license.getIssuedAtAsInstant().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES));
		var isTrialLicense = getEntitlements().showTrialHint();
		if (hasNotBeenIssuedRecently && !isTrialLicense) {
			LOG.debug("License was issued more than 5 minutes ago. Attempting a refresh to ensure we have the latest license information from the license server.");
			try {
				refreshLicense();
			} catch (IOException e) {
				LOG.error("Failed to refresh license during startup.", e);
			}
		}
	}

	/**
	 * Makes sure a valid (but possibly expired) license exists.
	 * <p>
	 * Called during {@link org.cryptomator.hub.Main application startup}.
	 *
	 * @throws JWTVerificationException if the license is invalid
	 */
	@Transactional
	@WithSpan("LicenseHolder.ensureLicenseExists")
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
	@WithSpan("LicenseHolder.requestAnonTrialLicense")
	DecodedJWT requestAnonTrialLicense(Settings settings) throws WebApplicationException {
		LOG.info("No license found. Requesting trial license...");
		var solution = solveChallenge();
		var trialResponse = licenseApi.generateTrialLicense(solution.toCaptcha());
		var validated = licenseValidator.validate(trialResponse.licenseKey(), trialResponse.hubId());
		settings.setLicenseKey(trialResponse.licenseKey());
		settings.setHubId(trialResponse.hubId());
		settingsRepo.persistAndFlush(settings);
		LOG.info("Successfully retrieved trial license.");
		return validated;
	}

	LicenseApi.Solution solveChallenge() {
		if (managedApiUsername.isPresent() && managedApiPassword.isPresent()) {
			var authHeader = "Basic " + Base64.getEncoder().encodeToString((managedApiUsername.get() + ":" + managedApiPassword.get()).getBytes(StandardCharsets.UTF_8));
			try {
				return licenseApi.generatePresolvedChallenge(authHeader);
			} catch (WebApplicationException e) {
				LOG.warn("Failed to retrieve presolved challenge for license refresh. Falling back to solving a regular challenge.", e);
			}
		}
		var challenge = licenseApi.generateChallenge();
		return solveChallenge(challenge);
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
		LOG.debug("Solving challenge...");
		long start = System.nanoTime();
		for (int i = 0; i < challenge.maxnumber(); i++) {
			var saltedSecret = challenge.salt() + i;
			sha256.update(saltedSecret.getBytes(StandardCharsets.US_ASCII));
			var attempt = hex.formatHex(sha256.digest());
			if (challenge.challenge().equals(attempt)) {
				long took = System.nanoTime() - start;
				LOG.debugv("Solved challenge in {1,number,integer} ms", took / 1_000_000);
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
		this.license = licenseValidator.validate(token, settings.getHubId());
		settings.setLicenseKey(token);
		settingsRepo.persistAndFlush(settings);
	}

	/**
	 * Attempts to refresh the Hub license every day between 01:00:00 and 02:00:00 AM UTC if claim refreshURL is present.
	 */
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
	@RunOnVirtualThread
	void scheduleLicenseRefresh() {
		try {
			randomSleeper.sleep(0, 59, ChronoUnit.MINUTES); // add random sleep to reduce infrastructure load
			refreshLicense();
		} catch (IOException e) {
			LOG.error("Scheduled license refresh failed.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn("Scheduled license refresh was interrupted.", e);
		}
	}

	public void refreshLicense() throws IOException {
		var refreshUrl = getLicenseRefreshUri();
		final String refreshedLicense;
		try {
			refreshedLicense = requestLicenseRefresh(refreshUrl, get().getToken());
		} catch (LicenseRefreshFailedException e) {
			LOG.errorv("Failed to refresh license token. Request to {0} was answered with response code {1,number,integer}", refreshUrl, e.statusCode);
			throw new IOException("Failed to refresh license token.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedIOException("License refresh was interrupted");
		}
		try {
			set(refreshedLicense);
		} catch (JWTVerificationException e) {
			LOG.errorv(e, "Failed to refresh license token. Refreshed token is invalid: {0}", refreshedLicense);
			throw new IOException("Invalid new license token.", e);
		}
	}

	private URI getLicenseRefreshUri() {
		var refreshUrlClaim = get().getClaim("refreshUrl");
		if (refreshUrlClaim.isMissing()) {
			throw new IllegalStateException("Missing refreshUrl claim.");
		}
		try {
			return new URI(refreshUrlClaim.asString());
		} catch (NullPointerException | URISyntaxException e) {
			throw new IllegalStateException("Invalid value for refreshUrl claim", e);
		}
	}

	//visible for testing
	String requestLicenseRefresh(URI refreshUrl, String licenseToken) throws InterruptedException, IOException, LicenseRefreshFailedException {
		var solution = solveChallenge();
		try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
			var body = "token=" + URLEncoder.encode(licenseToken, StandardCharsets.UTF_8)
					+ "&captcha=" + URLEncoder.encode(solution.toCaptcha(), StandardCharsets.UTF_8);
			var request = HttpRequest.newBuilder() //
					.uri(refreshUrl) //
					.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED) //
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
		return license;
	}

	public HubLicenseEntitlements getEntitlements() {
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
