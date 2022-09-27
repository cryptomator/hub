package org.cryptomator.hub.validation;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Pattern(regexp = "[-_=A-Za-z0-9]+\\.[-_=A-Za-z0-9]*\\.[-_=A-Za-z0-9]*\\.[-_=A-Za-z0-9]+\\.[-_=A-Za-z0-9]*")
@NotNull
public @interface IsJWE {
}
