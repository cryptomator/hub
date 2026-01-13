package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubLicenseEntitlements(@JsonProperty("seats") long seats, @JsonProperty("auditLogRetentionDays") long auditLogRetentionDays, @JsonProperty("iosLicense") String iosLicense, @JsonProperty("androidLicense") String androidLicense) {

	/**
	 * Calculates the earliest point of time for audit log entries to still be retained.
	 * @return {@link #auditLogRetentionDays} days in the past from now
	 */
	public Instant auditLogRetentionThreshold() {
		try {
			return Instant.now().minus(auditLogRetentionDays(), ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		} catch (ArithmeticException e) {
			return Instant.MIN;
		}
	}

}
