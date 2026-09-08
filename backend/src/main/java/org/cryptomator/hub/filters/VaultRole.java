package org.cryptomator.hub.filters;

import jakarta.ws.rs.NameBinding;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.keycloak.RealmRole;

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
	 * Which roles grant access to the annotated resource.
	 * <p>
	 * The user accessing the resource must have at least one of the listed vault roles.
	 * If empty, access is denied unless the check is bypassed by another rule.
	 *
	 * @return Roles required to access the annotated resource. Access is granted if _any_ role is present.
	 */
	VaultAccess.Role[] value() default {VaultAccess.Role.MEMBER};

	/**
	 * @return Name of the path parameter containing the {@link org.cryptomator.hub.entities.Vault#getId() vault id}.
	 */
	String vaultIdParam() default DEFAULT_VAULT_ID_PARAM;

	/**
	 * @return How to treat the case when a vault does not exist.
	 */
	OnMissingVault onMissingVault() default @OnMissingVault(OnMissingVault.Action.FORBIDDEN);

	@interface OnMissingVault {
		Action value() default Action.FORBIDDEN;

		enum Action {FORBIDDEN, NOT_FOUND, PASS, REQUIRE_REALM_ROLE}

		/**
		 * Which additional realm role is required to access the annotated resource.
		 * <p>
		 * Only relevant if {@link #value()} is set to {@link Action#REQUIRE_REALM_ROLE}.
		 *
		 * @return realm role required to access the annotated resource.
		 */
		RealmRole realmRole() default RealmRole.ADMIN;
	}

	/**
	 * What realm roles allow a user to skip the vault role check.
	 * <p>
	 * Only applies when the vault exists. For non-existing vaults, use {@link #onMissingVault()} instead.
	 *
	 * @return realm roles that bypasses the vault role check.
	 */
	RealmRole[] bypassForRealmRole() default {};

	/**
	 * If set to true, skip the role check if the current user is a member of this vault's emergency access council.
	 *
	 * @return whether emergency access council members should bypass the role check.
	 */
	boolean bypassForEmergencyAccess() default false;
}
