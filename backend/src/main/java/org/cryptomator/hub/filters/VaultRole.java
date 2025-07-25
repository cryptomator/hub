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
	 * @return Name of the path parameter containing the {@link org.cryptomator.hub.entities.Vault#id vault id}.
	 */
	String vaultIdParam() default DEFAULT_VAULT_ID_PARAM;

	/**
	 * @return How to treat the case when a vault does not exist.
	 */
	OnMissingVault onMissingVault() default OnMissingVault.FORBIDDEN;
	enum OnMissingVault { FORBIDDEN, NOT_FOUND, PASS, REQUIRE_REALM_ROLE }

	/**
	 * Which additional realm role is required to access the annotated resource.
	 *
	 * Only relevant if {@link #onMissingVault()} is set to {@link OnMissingVault#REQUIRE_REALM_ROLE}.
	 * @return realm role required to access the annotated resource.
	 */
	String realmRole() default "";

	/**
	 * If set to true, skip the role check if the current user is a member of this vault's emergency access council.
	 * @return whether emergency access council members should bypass the role check.
	 */
	boolean bypassForEmergencyAccess() default false;
}
