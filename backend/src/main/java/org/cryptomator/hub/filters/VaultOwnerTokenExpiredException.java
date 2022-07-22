package org.cryptomator.hub.filters;

import javax.ws.rs.ForbiddenException;

public class VaultOwnerTokenExpiredException extends ForbiddenException {

	public VaultOwnerTokenExpiredException(String message) {
		super(message);
	}

	public VaultOwnerTokenExpiredException(String message, Throwable cause) {
		super(message, cause);
	}
}
