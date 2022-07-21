package org.cryptomator.hub.filter;

import javax.ws.rs.NotAuthorizedException;

public class VaultOwnerNotProvidedException extends NotAuthorizedException {

	public VaultOwnerNotProvidedException(String message) {
		super(message);
	}

	public VaultOwnerNotProvidedException(String message, Throwable cause) {
		super(message, cause);
	}
}
