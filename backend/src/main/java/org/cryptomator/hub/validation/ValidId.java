package org.cryptomator.hub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Regex constraint for id fields requiring at least one alphanumerical ASCII char, dash (-), underscore (_) or dot (.) and nothing else.
 * Like {@link Pattern}, it accepts {@code null}.
 */
@Pattern(regexp = "[-_.a-zA-Z0-9]+")
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidId {

	String message() default "Input is empty or contains other characters than [-_.a-zA-Z0-9]";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}

