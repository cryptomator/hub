package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "license-api")
public interface LicenseApi {

	@GET
	@Path("/trial/challenge")
	@Produces(MediaType.APPLICATION_JSON)
	Challenge generateTrialChallenge(@QueryParam("hubId") String hubId);

	@POST
	@Path("/trial/verify")
	@Produces(MediaType.TEXT_PLAIN)
	String verifyTrialChallenge(@QueryParam("hubId") String hubId, @QueryParam("solution") int solution);

	record Challenge(@JsonProperty("salt") byte[] salt, @JsonProperty("counter") int counter, @JsonProperty("digest") byte[] digest, @JsonProperty("minCounter") int minCounter, @JsonProperty("maxCounter") int maxCounter) {
		public Challenge withoutSolution() {
			return new Challenge(salt, 0, digest, minCounter, maxCounter);
		}
	}

}
