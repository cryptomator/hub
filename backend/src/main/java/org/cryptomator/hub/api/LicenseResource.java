package org.cryptomator.hub.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;
import java.util.Optional;

@Path("/license")
public class LicenseResource {

	@Inject
	LicenseHolder licenseHolder;

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("user")
	@Operation(summary = "Get license status information", description = "Information includes the license expiration date, the licensed seats and the already used seats")
	@APIResponse(responseCode = "200")
	public LicenseStatusDto get() {
		return LicenseStatusDto.create(licenseHolder);
	}


	public record LicenseStatusDto(@JsonProperty("licensedSeats") Integer licensedSeats,
								   @JsonProperty("usedSeats") Integer usedSeats,
								   @JsonProperty("expiresAt") Instant expiresAt) {

		public static LicenseStatusDto create(LicenseHolder licenseHolder) {
			var licensedSeats = (int) licenseHolder.getSeats();
			var usedSeats = (int) EffectiveVaultAccess.countSeatOccupyingUsers();
			var expiresAt = Optional.ofNullable(licenseHolder.get()).map(DecodedJWT::getExpiresAtAsInstant).orElse(null);
			return new LicenseStatusDto(licensedSeats, usedSeats, expiresAt);
		}

	}

}
