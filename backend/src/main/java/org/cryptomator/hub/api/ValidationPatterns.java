package org.cryptomator.hub.api;

public final class ValidationPatterns {

	public static final String ID_PATTERN = "[-\\w]+";
	public static final String NAME_PATTERN = "(?U)[-_\\p{Alpha}\\p{Digit}\\x20]+";

	private ValidationPatterns() {
	}
}
