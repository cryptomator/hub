package org.cryptomator.hub.api;

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

@Path("/license")
public class LicenseResource {

	@Inject
	LicenseHolder licenseHolder;

	@Inject
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@GET
	@Path("/user-info")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("user")
	@Operation(summary = "Get license information for regular users", description = "Information includes the licensed seats, the already used seats and if defined, the license expiration date.")
	@APIResponse(responseCode = "200")
	public LicenseUserInfoDto get() {
		int usedSeats = (int) effectiveVaultAccessRepo.countSeatOccupyingUsers();
		return LicenseUserInfoDto.create(licenseHolder, usedSeats);
	}

	public record LicenseUserInfoDto(@JsonProperty("licensedSeats") Integer licensedSeats,
									 @JsonProperty("usedSeats") Integer usedSeats,
									 @JsonProperty("expiresAt") Instant expiresAt) {

		public static LicenseUserInfoDto create(LicenseHolder licenseHolder, int usedSeats) {
			var licensedSeats = (int) licenseHolder.getSeats();
			var expiresAt = licenseHolder.get().getExpiresAtAsInstant();
			return new LicenseUserInfoDto(licensedSeats, usedSeats, expiresAt);
		}

	}

}
