package org.cryptomator.hub.filters;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.VaultAccess;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Request filter which checks if the user's {@link org.cryptomator.hub.entities.VaultAccess.Role Role} on a {@link org.cryptomator.hub.entities.Vault Vault}.
 * <p>
 * Applied to all methods annotated with {@link VaultRole}.
 */
@Provider
@VaultRole
public class VaultRoleFilter implements ContainerRequestFilter {

	@Inject
	JsonWebToken jwt;

	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws ForbiddenException, NotAuthorizedException {
		var annotation = resourceInfo.getResourceMethod().getAnnotation(VaultRole.class);
		var vaultIdStr = requestContext.getUriInfo().getPathParameters().getFirst(annotation.vaultIdParam());
		final UUID vaultId;
		try {
			vaultId = UUID.fromString(vaultIdStr);
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new ForbiddenException("@VaultRole not set up correctly (unknown vault id)", e);
		}

		var userId = jwt.getSubject();
		if (userId == null) {
			throw new NotAuthorizedException("No JWT supplied in request header");
		}

		var effectiveRoles = EffectiveVaultAccess.listRoles(vaultId, userId);
		if (Arrays.stream(annotation.value()).noneMatch(effectiveRoles::contains)) {
			throw new ForbiddenException("Vault role required: " + Arrays.stream(annotation.value()).map(VaultAccess.Role::name).collect(Collectors.joining(", ")));
		}
	}
}
