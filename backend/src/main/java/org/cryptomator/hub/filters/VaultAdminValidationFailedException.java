package org.cryptomator.hub.filters;

import jakarta.ws.rs.BadRequestException;

public class VaultAdminValidationFailedException extends BadRequestException {

	public VaultAdminValidationFailedException(String message) {
		super(message);
	}

	public VaultAdminValidationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
