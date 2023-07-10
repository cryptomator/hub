package org.cryptomator.hub.filters;

import jakarta.ws.rs.NameBinding;
import org.cryptomator.hub.entities.VaultAccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add the {@link VaultRoleFilter} request filter to annotated service.
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface VaultRole {
	String DEFAULT_VAULT_ID_PARAM = "vaultId";

	/**
	 * @return Roles required to access the annotated resource. Access is granted if _any_ role is present.
	 */
	VaultAccess.Role[] value() default { VaultAccess.Role.MEMBER };

	/**
	 * @return Name of the path parameter containing the {@link org.cryptomator.hub.entities.Vault#id vault id}
	 */
	String vaultIdParam() default DEFAULT_VAULT_ID_PARAM;
}
