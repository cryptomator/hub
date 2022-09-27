package org.cryptomator.hub.validation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Pattern(regexp = "(?Ui)[-_\\w\\s]+")
@NotBlank
public @interface AlmostSafeText {
}
