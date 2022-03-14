package org.cryptomator.hub.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

record ConstraintViolationDto(@JsonProperty("errorType") String errorType, @JsonProperty("constraint") String constraint) {
	public static final String ERROR_TYPE = "ConstraintViolation";

	public ConstraintViolationDto(String constraint) {
		this(ERROR_TYPE, constraint);
	}
}
