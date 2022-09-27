package org.cryptomator.hub.validation;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
@NotNull
public @interface IsUUID {

}
