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
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.validation.ValidJWS;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;

//TODO: redirect ot /license path
@Path("/billing")
public class BillingResource {

	private final LicenseHolder licenseHolder;
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@Inject
	BillingResource(LicenseHolder licenseHolder, EffectiveVaultAccess.Repository effectiveVaultAccessRepo) {
		this.licenseHolder = licenseHolder;
		this.effectiveVaultAccessRepo = effectiveVaultAccessRepo;
	}

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
		var licenseToken = licenseHolder.get();
		return BillingDto.fromDecodedJwt(licenseToken, usedSeats, isManaged);
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
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
	}

	public record BillingDto(@JsonProperty("hubId") String hubId, @JsonProperty("email") String email,
							 @JsonProperty("licensedSeats") Integer licensedSeats, @JsonProperty("usedSeats") Integer usedSeats,
							 @JsonProperty("issuedAt") Instant issuedAt, @JsonProperty("expiresAt") Instant expiresAt, @JsonProperty("managedInstance") Boolean managedInstance, 
							 @JsonProperty("licenseKey") String licenseKey) {

		public static BillingDto fromDecodedJwt(DecodedJWT jwt, int usedSeats, boolean isManaged) {
			var id = jwt.getId();
			var email = jwt.getSubject();
			var licensedSeats = jwt.getClaim("seats").asInt(); // TODO eventually replace with "org.cryptomator.hub.entitlements"."seats", see https://github.com/cryptomator/hub/issues/391
			var issuedAt = jwt.getIssuedAt().toInstant();
			var expiresAt = jwt.getExpiresAt().toInstant();
			var licenseKey = jwt.getToken();
			return new BillingDto(id, email, licensedSeats, usedSeats, issuedAt, expiresAt, isManaged, licenseKey);
		}

	}

}
