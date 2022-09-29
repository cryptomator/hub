package org.cryptomator.hub.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Pattern(regexp = "(?Ui)[-_\\w\\s]+")
@NotBlank
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE_USE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidName {
	String message() default "Input is does not satisfy the regular expression (?Ui)[-_\\w\\s]+";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
