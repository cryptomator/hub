package org.cryptomator.hub.validation;

import javax.validation.constraints.Pattern;

/**
 * TODO: doc doc doc
 */
@Pattern(regexp = "[-_.a-zA-Z0-9]*")
public @interface SafeText {

}

