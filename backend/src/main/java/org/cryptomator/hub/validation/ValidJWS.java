package org.cryptomator.hub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Pattern(regexp = "[-_A-Za-z0-9]+=*\\.[-_A-Za-z0-9]*=*\\.[-_A-Za-z0-9]*=*")
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidJWS {

	String message() default "Input is not a valid JWS in compact serialization";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
