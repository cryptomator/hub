package org.cryptomator.hub.api;

import javax.ws.rs.ForbiddenException;

public class VaultOwnerNotProvidedException extends ForbiddenException {

	public VaultOwnerNotProvidedException(String message) {
		super(message);
	}

	public VaultOwnerNotProvidedException(String message, Throwable cause) {
		super(message, cause);
	}
}
