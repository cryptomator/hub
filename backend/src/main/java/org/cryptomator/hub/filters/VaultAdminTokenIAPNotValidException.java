package org.cryptomator.hub.filters;

import javax.ws.rs.ForbiddenException;

public class VaultAdminTokenIAPNotValidException extends ForbiddenException {

	public VaultAdminTokenIAPNotValidException(String message) {
		super(message);
	}

	public VaultAdminTokenIAPNotValidException(String message, Throwable cause) {
		super(message, cause);
	}
}
