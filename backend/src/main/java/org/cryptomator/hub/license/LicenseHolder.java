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
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class LicenseHolder {

	private static final Logger LOG = Logger.getLogger(LicenseHolder.class);

	private final Boolean managedInstance;
	private final Optional<String> initialId;
	private final Optional<String> initialLicenseToken;
	private final Optional<String> managedApiUsername;
	private final Optional<String> managedApiPassword;
	private final LicenseValidator licenseValidator;
	private final RandomSleeper randomSleeper;
	private final Settings.Repository settingsRepo;
	private final LicenseApi licenseApi;

	private final AtomicReference<@Nullable DecodedJWT> licenseRef = new AtomicReference<>();
	private @Nullable DecodedJWT unconfiguredLicense;

	@Inject
	LicenseHolder(@ConfigProperty(name = "hub.managed-instance", defaultValue = "false") Boolean managedInstance,
				  @ConfigProperty(name = "hub.initial-id") Optional<String> initialId,
				  @ConfigProperty(name = "hub.initial-license") Optional<String> initialLicenseToken,
				  @ConfigProperty(name = "hub.managed-api-username") Optional<String> managedApiUsername,
				  @ConfigProperty(name = "hub.managed-api-password") Optional<String> managedApiPassword,
				  LicenseValidator licenseValidator,
				  RandomSleeper randomSleeper,
				  Settings.Repository settingsRepo,
				  @RestClient LicenseApi licenseApi) {
		this.managedInstance = managedInstance;
		this.initialId = initialId;
		this.initialLicenseToken = initialLicenseToken;
		this.managedApiUsername = managedApiUsername;
		this.managedApiPassword = managedApiPassword;
		this.licenseValidator = licenseValidator;
		this.randomSleeper = randomSleeper;
		this.settingsRepo = settingsRepo;
		this.licenseApi = licenseApi;
	}

	@PostConstruct
	void init() {
		var license = this.loadLicense();
		this.licenseRef.set(license);
		if (isSetupRequired()) {
			return; // no license configured yet — skip refresh attempts, an admin will obtain a license via the setup workflow
		}
		// refresh upon startup:
		// except for trial licenses and recently issued licenses (to avoid restart-loop spam)
		var hasNotBeenIssuedRecently = license.getIssuedAtAsInstant().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES));
		var isTrialLicense = getEntitlements().showTrialHint();
		if (hasNotBeenIssuedRecently && !isTrialLicense) {
			LOG.debug("License was issued more than 5 minutes ago. Attempting a refresh to ensure we have the latest license information from the license server.");
			try {
				refreshLicense();
			} catch (LicenseRefreshFailedException e) {
				LOG.error("Failed to refresh license during startup.", e);
			}
		}
	}

	/**
	 * Loads the license during application startup: from the database if present, otherwise from the {@code hub.initial-license} config property.
	 * If neither is available (or validation fails), the app starts in setup mode with an {@link UnconfiguredLicense} — this method never throws.
	 *
	 * @return a valid (but possibly expired) license or the placeholder license indicating that {@link #isSetupRequired() setup is required}
	 */
	@Transactional
	@WithSpan("LicenseHolder.loadLicense")
	DecodedJWT loadLicense() {
		var settings = settingsRepo.get();
		this.unconfiguredLicense = UnconfiguredLicense.create(settings.getHubId());
		if (settings.getLicenseKey() != null) {
			try {
				return validateExistingLicense(settings);
			} catch (JWTVerificationException e) {
				LOG.errorv(e, "License stored in the database is invalid or does not match hub ID {0}. Starting in setup mode; an admin needs to provide a valid license via the web interface.", settings.getHubId());
				return unconfiguredLicense; // keep the stored license key untouched, so it remains available for inspection
			}
		} else if (initialLicenseToken.isPresent() && initialId.isPresent()) {
			try {
				return validateAndApplyInitLicense(settings, initialLicenseToken.get(), initialId.get());
			} catch (JWTVerificationException e) {
				LOG.error("Failed to apply license from property hub.initial-license. Starting in setup mode; an admin needs to provide a valid license via the web interface.", e);
				return unconfiguredLicense;
			}
		} else {
			LOG.warn("No license configured. Starting in setup mode; an admin needs to obtain a license via the web interface.");
			return unconfiguredLicense;
		}
	}

	/**
	 * Indicates whether this instance still runs with the built-in placeholder license, i.e. no real license has been configured yet.
	 *
	 * @return {@code true} while no real license is set
	 */
	public boolean isSetupRequired() {
		var license = this.licenseRef.get();
		return license != null && license == unconfiguredLicense;
	}

	@Transactional(Transactional.TxType.MANDATORY)
	DecodedJWT validateExistingLicense(Settings settings) throws JWTVerificationException {
		var license = settings.getLicenseKey();
		if (license == null) {
			throw new JWTVerificationException("No license");
		}
		try {
			var validated = licenseValidator.validate(license, settings.getHubId());
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
	 * Parses, verifies and persists the given token as the license in the database, adopting the given hub ID.
	 * <p>
	 * Used to install a trial license that was obtained from the license server by the browser: trial licenses are issued with a freshly minted hub ID,
	 * so this method is only permitted while {@link #isSetupRequired() setup is required}, protecting the hub ID an already configured license is bound to.
	 *
	 * @param token The string representation of the JWT license
	 * @param hubId The hub ID the license is issued for, replacing this instance's current hub ID
	 * @throws JWTVerificationException if the license cannot be verified or does not match the given hub ID
	 * @throws IllegalStateException    if a license is already configured
	 */
	@Transactional
	public synchronized void set(String token, String hubId) throws JWTVerificationException {
		if (!isSetupRequired()) {
			throw new IllegalStateException("A license is already configured");
		}
		var validated = licenseValidator.validate(token, hubId);
		var settings = settingsRepo.get();
		settings.setHubId(hubId);
		settings.setLicenseKey(token);
		settingsRepo.persistAndFlush(settings);
		this.licenseRef.set(validated);
	}

	/**
	 * Parses, verifies and persists the given token as the license in the database.
	 *
	 * @param token The string represenation of the JWT license
	 * @throws JWTVerificationException if the license cannot be verfied
	 */
	@Transactional
	public synchronized void set(String token) throws JWTVerificationException {
		var settings = settingsRepo.get();
		this.licenseRef.set(licenseValidator.validate(token, settings.getHubId()));
		settings.setLicenseKey(token);
		settingsRepo.persistAndFlush(settings);
	}

	/**
	 * Attempts to refresh the Hub license every day between 01:00:00 and 02:00:00 AM UTC if claim refreshURL is present.
	 */
	@Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
	@RunOnVirtualThread
	void scheduleLicenseRefresh() {
		if (isSetupRequired()) {
			LOG.debug("Skipping license refresh, no license configured yet.");
			return;
		}
		try {
			randomSleeper.sleep(0, 59, ChronoUnit.MINUTES); // add random sleep to reduce infrastructure load
			refreshLicense();
		} catch (LicenseRefreshFailedException e) {
			LOG.error("Scheduled license refresh failed.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn("Scheduled license refresh was interrupted.", e);
		}
	}

	public void refreshLicense(UUID session) throws LicenseRefreshFailedException, WebApplicationException {
		var refreshedLicense = licenseApi.getLicense(session);
		validateAndSet(refreshedLicense);
	}

	public void refreshLicense() throws LicenseRefreshFailedException {
		if (isSetupRequired()) {
			throw new LicenseRefreshFailedException("No license configured");
		}
		var refreshedLicense = requestLicenseRefresh(get().getToken());
		validateAndSet(refreshedLicense);
	}

	private void validateAndSet(String refreshedLicense) throws LicenseRefreshFailedException {
		try {
			set(refreshedLicense);
		} catch (JWTVerificationException e) {
			LOG.errorv(e, "Failed to refresh license token. Refreshed token is invalid: {0}", refreshedLicense);
			throw new LicenseRefreshFailedException("Invalid new license token.", e);
		}
	}

	//visible for testing
	String requestLicenseRefresh(String licenseToken) throws LicenseRefreshFailedException {
		var solution = solveChallenge();
		try {
			return licenseApi.refreshLicense(licenseToken, solution.toCaptcha());
		} catch (WebApplicationException e) {
			throw new LicenseRefreshFailedException("License endpoint responded with status code " + e.getResponse().getStatus());
		}
	}

	@NotNull
	public synchronized DecodedJWT get() {
		var license = this.licenseRef.get();
		if (license == null) {
			throw new IllegalStateException();
		}
		return license;
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
		return get().getExpiresAt().toInstant().isBefore(Instant.now());
	}

	public boolean isManagedInstance() {
		return managedInstance;
	}

	public static class LicenseRefreshFailedException extends Exception {

		LicenseRefreshFailedException(String message, Throwable cause) {
			super(message, cause);
		}

		LicenseRefreshFailedException(String message) {
			super(message);
		}

		LicenseRefreshFailedException(Throwable cause) {
			super(cause);
		}
	}

}
