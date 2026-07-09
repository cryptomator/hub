package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.filters.AvailableDuringSetup;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Path("/license")
public class LicenseResource {

	private final LicenseHolder licenseHolder;
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@Inject
	LicenseResource(LicenseHolder licenseHolder, EffectiveVaultAccess.Repository effectiveVaultAccessRepo) {
		this.licenseHolder = licenseHolder;
		this.effectiveVaultAccessRepo = effectiveVaultAccessRepo;
	}

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
			var licensedSeats = (int) licenseHolder.getEntitlements().seats();
			var expiresAt = licenseHolder.get().getExpiresAtAsInstant();
			return new LicenseUserInfoDto(licensedSeats, usedSeats, expiresAt);
		}

	}

	@POST
	@Path("/refresh")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@RolesAllowed("admin")
	@AvailableDuringSetup
	@Operation(summary = "Refresh license information", description = "Refreshes the license information from the license server.")
	@APIResponse(responseCode = "204", description = "License information refreshed")
	@APIResponse(responseCode = "404", description = "Session not found")
	@APIResponse(responseCode = "500", description = "License refresh failed")
	public Response refresh(@FormParam("session") @Nullable UUID session) {
		try {
			if (session == null) {
				licenseHolder.refreshLicense();
			} else {
				licenseHolder.refreshLicense(session);
			}
		} catch (LicenseHolder.LicenseRefreshFailedException e) {
			throw new InternalServerErrorException("License refresh failed", e);
		}
		return Response.noContent().build();
	}

}
