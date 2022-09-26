package org.cryptomator.hub.api;

public final class ValidationUtil {

	public static final String ID_PATTERN = "[-\\w]+";
	public static final String JWE_PATTERN = "[-_=.\\p{Alnunm}]+";
	public static final String NAME_PATTERN = "(?U)[-_\\p{Alnum}\\x20]+";

	private ValidationUtil() {
	}
}
