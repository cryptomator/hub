package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubLicenseEntitlements(@JsonProperty("seats") long seats,
									 @JsonProperty("showTrialHint") boolean showTrialHint,
									 @JsonProperty("auditLogRetentionDays") long auditLogRetentionDays,
									 @JsonProperty("iosLicense") String iosLicense,
									 @JsonProperty("androidLicense") String androidLicense) {
	/**
	 * Calculates the earliest point of time for audit log entries to still be retained.
	 *
	 * @return {@link #auditLogRetentionDays} days in the past from now
	 */
	public Instant auditLogRetentionThreshold() {
		try {
			return Instant.now().minus(auditLogRetentionDays(), ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		} catch (ArithmeticException e) {
			return Instant.MIN;
		}
	}

	// region Factory + Withers (should only be used in tests)

	public static HubLicenseEntitlements create() {
		return new HubLicenseEntitlements(0, false, 0, null, null);
	}

	public HubLicenseEntitlements withSeats(long seats) {
		return new HubLicenseEntitlements(seats, this.showTrialHint, this.auditLogRetentionDays, this.iosLicense, this.androidLicense);
	}

	public HubLicenseEntitlements withShowTrialHint(boolean showTrialHint) {
		return new HubLicenseEntitlements(this.seats, showTrialHint, this.auditLogRetentionDays, this.iosLicense, this.androidLicense);
	}

	public HubLicenseEntitlements withAuditLogRetentionDays(long auditLogRetentionDays) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, auditLogRetentionDays, this.iosLicense, this.androidLicense);
	}

	public HubLicenseEntitlements withIosLicense(String iosLicense) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, iosLicense, this.androidLicense);
	}

	public HubLicenseEntitlements withAndroidLicense(String androidLicense) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, this.iosLicense, androidLicense);
	}

	// endregion
}
