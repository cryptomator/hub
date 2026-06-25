package org.cryptomator.hub.license;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Translates certain http status codes to specific exception types.
 */
public class ExceptionMapper implements ResponseExceptionMapper<WebApplicationException> {

	@Override
	public WebApplicationException toThrowable(Response response) {
		return switch (response.getStatus()) {
			case 404 -> new NotFoundException();
			default -> new ServerErrorException("Received unexpected http status code: " + response.getStatus(), Response.Status.BAD_GATEWAY);
		};
	}
}
