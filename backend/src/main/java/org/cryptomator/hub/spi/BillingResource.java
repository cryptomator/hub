package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Billing;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/billing")
public class BillingResource {

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get the billing information")
	public BillingDto get() {
		return Billing.<Billing>findByIdOptional(0).map(BillingDto::fromEntity).get();
	}

	@PUT
	@Path("/token")
	@Consumes(MediaType.TEXT_PLAIN)
	@Transactional
	@Operation(summary = "set the token")
	public Response setToken(String token) {
		var billing = Billing.<Billing>findByIdOptional(0).get();
		billing.token = token;
		billing.persist();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	public record BillingDto(@JsonProperty("hub_id") String hubId, @JsonProperty("token") String token) {

		public static BillingDto fromEntity(Billing entity) {
			return new BillingDto(entity.hubId, entity.token);
		}

	}

}
