package org.cryptomator.hub.filters;

import javax.ws.rs.ForbiddenException;

public class VaultAdminTokenExpiredException extends ForbiddenException {

	public VaultAdminTokenExpiredException(String message) {
		super(message);
	}

	public VaultAdminTokenExpiredException(String message, Throwable cause) {
		super(message, cause);
	}
}
