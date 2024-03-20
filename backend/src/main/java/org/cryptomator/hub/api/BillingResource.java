package org.cryptomator.hub.api;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.EffectiveVaultAccessRepository;
import org.cryptomator.hub.entities.Settings;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.validation.ValidJWS;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;
import java.util.Optional;

@Path("/billing")
public class BillingResource {

	@Inject
	LicenseHolder licenseHolder;
	@Inject
	EffectiveVaultAccessRepository effectiveVaultAccessRepo;

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get the billing information")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "only admins are allowed to get the billing information")
	public BillingDto get() {
		int usedSeats = (int) effectiveVaultAccessRepo.countSeatOccupyingUsers();
		boolean isManaged = licenseHolder.isManagedInstance();
		return Optional.ofNullable(licenseHolder.get())
				.map(jwt -> BillingDto.fromDecodedJwt(jwt, usedSeats, isManaged))
				.orElseGet(() -> {
					var hubId = Settings.get().hubId;
					return BillingDto.create(hubId, (int) licenseHolder.getNoLicenseSeats(), usedSeats, isManaged);
				});
	}

	@PUT
	@Path("/token")
	@RolesAllowed("admin")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "set the token")
	@APIResponse(responseCode = "204", description = "token set")
	@APIResponse(responseCode = "400", description = "token is invalid (e.g., expired or invalid signature)")
	@APIResponse(responseCode = "403", description = "only admins are allowed to set the token")
	public Response setToken(@NotNull @ValidJWS String token) {
		try {
			licenseHolder.set(token);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (JWTVerificationException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	public record BillingDto(@JsonProperty("hubId") String hubId, @JsonProperty("hasLicense") Boolean hasLicense, @JsonProperty("email") String email,
							 @JsonProperty("licensedSeats") Integer licensedSeats, @JsonProperty("usedSeats") Integer usedSeats,
							 @JsonProperty("issuedAt") Instant issuedAt, @JsonProperty("expiresAt") Instant expiresAt, @JsonProperty("managedInstance") Boolean managedInstance) {

		public static BillingDto create(String hubId, int noLicenseSeatCount, int usedSeats, boolean isManaged) {
			return new BillingDto(hubId, false, null, noLicenseSeatCount, usedSeats, null, null, isManaged);
		}

		public static BillingDto fromDecodedJwt(DecodedJWT jwt, int usedSeats, boolean isManaged) {
			var id = jwt.getId();
			var email = jwt.getSubject();
			var licensedSeats = jwt.getClaim("seats").asInt();
			var issuedAt = jwt.getIssuedAt().toInstant();
			var expiresAt = jwt.getExpiresAt().toInstant();
			return new BillingDto(id, true, email, licensedSeats, usedSeats, issuedAt, expiresAt, isManaged);
		}

	}

}
