package org.cryptomator.hub.api;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class GoneException extends ClientErrorException {
	public GoneException() {
		super(Response.Status.GONE);
	}

	public GoneException(String message) {
		super(message, Response.Status.GONE);
	}


	public GoneException(Throwable cause) {
		super(Response.Status.GONE, cause);
	}

	public GoneException(String message, Throwable cause) {
		super(message, Response.Status.GONE, cause);
	}

}
