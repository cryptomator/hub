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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Client for the license server.
 * <p>
 * Every method returns a {@link CompletionStage}, so that a failed request can not slip past a caller as an unchecked exception:
 * results can be consumed via {@link #await(CompletionStage)}, which reports any failure - an unreachable license server just as much as a non-2xx response -
 * as a checked {@link RequestFailedException}.
 */
@RegisterRestClient(configKey = "license-api")
@RegisterProvider(ExceptionMapper.class)
public interface LicenseApi {

	@GET
	@Path("/hub/challenge")
	@Produces(MediaType.APPLICATION_JSON)
	CompletionStage<Challenge> generateChallenge();

	@GET
	@Path("/hub/no-challenge")
	@Produces(MediaType.APPLICATION_JSON)
	CompletionStage<Solution> generatePresolvedChallenge(@HeaderParam("Authorization") String authHeader);

	@POST
	@Path("/hub/refresh")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	CompletionStage<String> refreshLicense(@FormParam("token") String licenseKey, @FormParam("captcha") String captcha);

	@GET
	@Path("/hub")
	@Produces(MediaType.TEXT_PLAIN)
	CompletionStage<String> getLicense(@QueryParam("session") UUID sessionId);

	/**
	 * Blocks until the given request has completed and returns its result.
	 *
	 * @param request the pending request
	 * @param <T>     the type of the requested resource
	 * @return the result of the request
	 * @throws RequestFailedException if the request failed, e.g. because the license server is unreachable or did not respond with a 2xx status code
	 */
	static <T> T await(CompletionStage<T> request) throws RequestFailedException {
		try {
			return request.toCompletableFuture().join();
		} catch (CompletionException e) {
			throw new RequestFailedException(e.getCause());
		}
	}

	/**
	 * Indicates that a request to the license server did not yield a result.
	 * <p>
	 * The {@link #getCause() cause} is a {@link WebApplicationException} if the license server was reached but responded with an unexpected status code
	 * (see {@link ExceptionMapper}), and the transport-level failure otherwise (i.e. ProcessingException).
	 */
	class RequestFailedException extends Exception {

		RequestFailedException(Throwable cause) {
			super(describe(cause), cause);
		}

		private static String describe(Throwable cause) {
			if (cause instanceof WebApplicationException e) {
				return "License server responded with status code " + e.getResponse().getStatus();
			} else {
				return "Failed to reach the license server";
			}
		}
	}

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

}
