package org.cryptomator.hub.api;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

class PaymentRequiredException extends ClientErrorException {
	public PaymentRequiredException() {
		super(Response.Status.PAYMENT_REQUIRED);
	}

	public PaymentRequiredException(String message) {
		super(message, Response.Status.PAYMENT_REQUIRED);
	}

	public PaymentRequiredException(Throwable cause) {
		super(Response.Status.PAYMENT_REQUIRED, cause);
	}

	public PaymentRequiredException(String msg, Throwable cause) {
		super(msg, Response.Status.PAYMENT_REQUIRED, cause);
	}

}
