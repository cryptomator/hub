package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
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
	Challenge generateTrialChallenge();

	@POST
	@Path("/trial/verify")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	String verifyTrialChallenge(@QueryParam("hubId") String hubId, Solution solution);

	record Challenge(@JsonProperty("algorithm") String algorithm,
							@JsonProperty("challenge") String challenge,
							@JsonProperty("maxnumber") int maxnumber,
							@JsonProperty("salt") String salt,
							@JsonProperty("signature") String signature) {
		public Solution solve(int number) {
			return new Solution(algorithm, challenge, number, salt, signature);
		}
	}

	record Solution(@JsonProperty("algorithm") String algorithm,
						   @JsonProperty("challenge") String challenge,
						   @JsonProperty("number") int number,
						   @JsonProperty("salt") String salt,
						   @JsonProperty("signature") String signature) {
	}


}
