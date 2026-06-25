package org.cryptomator.hub.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.UUID;

@RegisterRestClient(configKey = "license-api")
@RegisterProvider(ExceptionMapper.class)
public interface LicenseApi {

	@GET
	@Path("/hub/challenge")
	@Produces(MediaType.APPLICATION_JSON)
	Challenge generateChallenge();

	@GET
	@Path("/hub/no-challenge")
	@Produces(MediaType.APPLICATION_JSON)
	Solution generatePresolvedChallenge(@HeaderParam("Authorization") String authHeader);

	@POST
	@Path("/hub/trial")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	TrialLicenseResponse generateTrialLicense(@FormParam("captcha") String captcha);

	@POST
	@Path("/hub/refresh")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	String refreshLicense(@FormParam("token") String licenseKey, @FormParam("captcha") String captcha);

	@GET
	@Path("/hub")
	@Produces(MediaType.TEXT_PLAIN)
	String getLicense(@QueryParam("session") UUID sessionId);

	record Challenge(@JsonProperty("algorithm") String algorithm,
					 @JsonProperty("challenge") String challenge,
					 @JsonProperty("maxnumber") int maxnumber,
					 @JsonProperty("salt") String salt,
					 @JsonProperty("signature") String signature) {
		public Solution solve(int number, long took) {
			return new Solution(algorithm, challenge, number, salt, signature, took);
		}
	}

	record Solution(@JsonProperty("algorithm") String algorithm,
					@JsonProperty("challenge") String challenge,
					@JsonProperty("number") int number,
					@JsonProperty("salt") String salt,
					@JsonProperty("signature") String signature,
					@JsonProperty("took") long took) {
		private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

		public String toCaptcha() {
			try {
				var serialized = OBJECT_MAPPER.writer().writeValueAsBytes(this);
				return Base64.getEncoder().encodeToString(serialized);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to encode captcha", e);
			}
		}
	}

	record TrialLicenseResponse(@JsonProperty("hubId") String hubId,
								@JsonProperty("licenseKey") String licenseKey) {
	}


}
