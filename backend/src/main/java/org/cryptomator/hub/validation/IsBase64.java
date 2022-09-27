package org.cryptomator.hub.validation;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Pattern(regexp = "[+/=A-Za-z0-9]+")
@NotNull
public @interface IsBase64 {
}
