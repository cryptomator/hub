package org.cryptomator.hub.api;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Billing;
import org.cryptomator.hub.license.LicenseValidator;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

@Path("/billing")
public class BillingResource {

	@Inject
	LicenseValidator licenseValidator;

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get the billing information")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "only admins are allowed to get the billing information")
	public BillingDto get() {
		return Billing.<Billing>findByIdOptional(0).map(BillingDto::fromEntity).get();
	}

	@PUT
	@Path("/token")
	@RolesAllowed("admin")
	@Consumes(MediaType.TEXT_PLAIN)
	@Transactional
	@Operation(summary = "set the token")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "400", description = "token is invalid (e.g., expired or invalid signature)")
	@APIResponse(responseCode = "403", description = "only admins are allowed to set the token")
	public Response setToken(String token) {
		var billing = Billing.<Billing>findByIdOptional(0).get();
		try {
			licenseValidator.validate(token, billing.hubId);
		} catch (JWTVerificationException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		billing.token = token;
		billing.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	public record BillingDto(@JsonProperty("hubId") String hubId, @JsonProperty("hasLicense") Boolean hasLicense, @JsonProperty("email") String email,
							 @JsonProperty("totalSeats") Integer totalSeats, @JsonProperty("remainingSeats") Integer remainingSeats,
							 @JsonProperty("issuedAt") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Date issuedAt,
							 @JsonProperty("expiresAt") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Date expiresAt) {

		public static BillingDto fromEntity(Billing entity) {
			if (entity.token == null) {
				return new BillingDto(entity.hubId, false, null, null, null, null, null);
			}
			var licenseValidator = new LicenseValidator();
			var jwt = licenseValidator.validate(entity.token, entity.hubId);
			var email = jwt.getSubject();
			var totalSeats = jwt.getClaim("seats").asInt();
			var remainingSeats = totalSeats; // TODO
			var issuedAt = jwt.getIssuedAt();
			var expiresAt = jwt.getExpiresAt();
			return new BillingDto(entity.hubId, true, email, totalSeats, remainingSeats, issuedAt, expiresAt);
		}

	}

}
