package org.cryptomator.hub.api;

import jakarta.ws.rs.core.Response;

public class ErrorCodeException extends RuntimeException {

	private final ErrorCode errorCode;

	public ErrorCodeException(ErrorCode errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode;
	}

	public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.name(), cause);
		this.errorCode = errorCode;
	}

	public Response.Status getStatus() {
		return errorCode.getStatus();
	}
}
