package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubLicenseEntitlements(@JsonProperty("seats") long seats, @JsonProperty("auditLogRetentionDays") long auditLogRetentionDays, @JsonProperty("iosLicense") String iosLicense, @JsonProperty("androidLicense") String androidLicense) {

}
