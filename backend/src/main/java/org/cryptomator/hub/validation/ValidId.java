package org.cryptomator.hub.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * TODO: doc doc doc
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

