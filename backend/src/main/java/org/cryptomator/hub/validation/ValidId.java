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
 * Regex constraint for id fields allowing only alphanumerical ASCII chars, dash (-), underscore (_) and dot (.).
 */
@Pattern(regexp = "[-_.a-zA-Z0-9]*")
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidId {

	String message() default "Input contains other characters than [-_.a-zA-Z0-9]";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}

