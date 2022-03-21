package org.cryptomator.hub.license;

import javax.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Check validity of license before executing the annotated service. Fail with HTTP status code 402 if invalid.
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface LicenseCheck {

	/**
	 * Return an array of additional claims to check.
	 * @return Array of additional claims.
	 */
	LicenseClaim[] claims() default {};

}
