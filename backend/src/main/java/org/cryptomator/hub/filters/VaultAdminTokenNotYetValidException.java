package org.cryptomator.hub.filters;

import javax.ws.rs.ForbiddenException;

public class VaultAdminTokenNotYetValidException extends ForbiddenException {

	public VaultAdminTokenNotYetValidException(String message) {
		super(message);
	}

	public VaultAdminTokenNotYetValidException(String message, Throwable cause) {
		super(message, cause);
	}
}
