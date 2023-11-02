package org.cryptomator.hub.api;

import jakarta.ws.rs.ClientErrorException;

class ActionRequiredException extends ClientErrorException {
	public static final int STATUS = 449;

	public ActionRequiredException() {
		super(STATUS);
	}

	public ActionRequiredException(String message) {
		super(message, STATUS);
	}

	public ActionRequiredException(Throwable cause) {
		super(STATUS, cause);
	}

	public ActionRequiredException(String msg, Throwable cause) {
		super(msg, STATUS, cause);
	}

}
