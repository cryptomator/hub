package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubLicenseEntitlements(@JsonProperty("seats") long seats,
									 @JsonProperty("showTrialHint") boolean showTrialHint,
									 @JsonProperty("auditLogRetentionDays") long auditLogRetentionDays,
									 @JsonProperty("emergencyAccessEnabled") boolean emergencyAccessEnabled,
									 @JsonProperty("keycloakAccessEnabled") boolean keycloakAccessEnabled,
									 @JsonProperty("iosLicense") @Nullable String iosLicense,
									 @JsonProperty("androidLicense") @Nullable String androidLicense,
									 @JsonProperty("desktopLicense") @Nullable String desktopLicense) {

	/**
	 * Calculates the earliest point of time for audit log entries to still be retained.
	 *
	 * @return {@link #auditLogRetentionDays} days in the past from now
	 */
	public Instant auditLogRetentionThreshold() {
		try {
			return Instant.now().minus(auditLogRetentionDays(), ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		} catch (ArithmeticException _) {
			return Instant.MIN;
		}
	}

	// region Factory + Withers

	public static HubLicenseEntitlements create() {
		return new HubLicenseEntitlements(0, false, 0, false, true, null, null, null);
	}

	public HubLicenseEntitlements withSeats(long seats) {
		return new HubLicenseEntitlements(seats, this.showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withShowTrialHint(boolean showTrialHint) {
		return new HubLicenseEntitlements(this.seats, showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withAuditLogRetentionDays(long auditLogRetentionDays) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withEmergencyAccessEnabled(boolean emergencyAccessEnabled) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withKeycloakAccessEnabled(boolean keycloakAccessEnabled) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, keycloakAccessEnabled, this.iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withIosLicense(@Nullable String iosLicense) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, iosLicense, this.androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withAndroidLicense(@Nullable String androidLicense) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, androidLicense, this.desktopLicense);
	}

	public HubLicenseEntitlements withDesktopLicense(@Nullable String desktopLicense) {
		return new HubLicenseEntitlements(this.seats, this.showTrialHint, this.auditLogRetentionDays, this.emergencyAccessEnabled, this.keycloakAccessEnabled, this.iosLicense, this.androidLicense, desktopLicense);
	}

	// endregion
}
