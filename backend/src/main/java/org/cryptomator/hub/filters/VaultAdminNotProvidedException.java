package org.cryptomator.hub.filters;

import javax.ws.rs.NotAuthorizedException;

public class VaultAdminNotProvidedException extends NotAuthorizedException {

	public VaultAdminNotProvidedException(String message) {
		super(message);
	}

	public VaultAdminNotProvidedException(String message, Throwable cause) {
		super(message, cause);
	}
}
