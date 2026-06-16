package org.cryptomator.hub.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorCodeExceptionMapper implements ExceptionMapper<ErrorCodeException> {

	@Override
	public Response toResponse(ErrorCodeException exception) {
		return Response.status(exception.getStatus())
				.entity(exception.getMessage())
				.type(MediaType.TEXT_PLAIN)
				.build();
	}
}
