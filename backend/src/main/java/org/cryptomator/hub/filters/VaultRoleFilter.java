package org.cryptomator.hub.filters;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.EmergencyRecoveryProcess;
import org.cryptomator.hub.entities.Vault;
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

	@Inject
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@Inject
	EmergencyRecoveryProcess.Repository recoveryRepo;

	@Inject
	Vault.Repository vaultRepo;

	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws NotFoundException, ForbiddenException, NotAuthorizedException {
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

		var vault = vaultRepo.findById(vaultId);
		if (vault != null && annotation.bypassForEmergencyAccess() && vault.getEmergencyKeyShares().containsKey(userId)) {
			// user is a member of the emergency access council, so we skip the role check:
			return;
		}

		boolean emergencyBypass = false;
		if (annotation.bypassForEmergencyAccess()) {
			if (vault != null && vault.getEmergencyKeyShares().containsKey(userId)) {
				emergencyBypass = true;
			} else {
				emergencyBypass = hasEmergencyProcessBypass(userId, vaultId);
			}
		}
		if (emergencyBypass) {
			return;
		}

		var forbiddenMsg = "Vault role required: " + Arrays.stream(annotation.value()).map(VaultAccess.Role::name).collect(Collectors.joining(", "));
		if (vault != null) {
			// check permissions for existing vault:
			var effectiveRoles = effectiveVaultAccessRepo.listRoles(vaultId, userId);
			if (Arrays.stream(annotation.value()).noneMatch(effectiveRoles::contains)) {
				throw new ForbiddenException(forbiddenMsg);
			}
		} else {
			// how to treat non-existing vault:
			switch (annotation.onMissingVault()) {
				case FORBIDDEN -> throw new ForbiddenException(forbiddenMsg);
				case NOT_FOUND -> throw new NotFoundException("Vault not found");
				case PASS -> {}
				case REQUIRE_REALM_ROLE -> {
					if (!requestContext.getSecurityContext().isUserInRole(annotation.realmRole())) {
						throw new ForbiddenException("Missing role " + annotation.realmRole());
					}
				}
			}
		}
	}

	private boolean hasEmergencyProcessBypass(String userId, UUID vaultId) {
		var processes = recoveryRepo.findByVaultId(vaultId);
		if (processes == null) {
			return false;
		}
		return processes.anyMatch(p -> {
			var my = p.getRecoveredKeyShares().get(userId);
			if (my == null || my.getRecoveredKeyShare() == null) {
				return false;
			}

			long recovered = p.getRecoveredKeyShares().values().stream()
					.filter(s -> s.getRecoveredKeyShare() != null)
					.count();
			return recovered >= p.getRequiredKeyShares();
		});
	}
}
