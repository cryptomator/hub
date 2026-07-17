package org.cryptomator.hub.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks JAX-RS resource classes or methods that stay reachable while no license is configured yet.
 * All other endpoints are rejected with status 402 by the {@link LicenseSetupFilter} until an admin completes the license setup.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AvailableDuringSetup {
}
