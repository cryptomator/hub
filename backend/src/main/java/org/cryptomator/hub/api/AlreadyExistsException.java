package org.cryptomator.hub.api;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class AlreadyExistsException extends ClientErrorException {

	public AlreadyExistsException() {
		super(Response.Status.CONFLICT);
	}

}
