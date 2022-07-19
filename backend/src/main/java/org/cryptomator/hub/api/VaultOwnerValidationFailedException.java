package org.cryptomator.hub.api;

import javax.ws.rs.BadRequestException;

public class VaultOwnerValidationFailedException extends BadRequestException {

	public VaultOwnerValidationFailedException(String message) {
		super(message);
	}

	public VaultOwnerValidationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
