package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.EmergencyRecoveryProcess;
import org.cryptomator.hub.entities.Vault;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.stream.Collectors;

@Path("/emergency-access")
@Produces(MediaType.APPLICATION_JSON)
public class EmergencyAccessParticipantResource {

    @Inject JsonWebToken jwt;
    @Inject EmergencyRecoveryProcess.Repository processRepo;
    @Inject Vault.Repository vaultRepo;

    @GET
    @Path("/my-process-vaults")
    @RolesAllowed("user")
    @Transactional
    public List<VaultResource.VaultDto> myProcessVaults() {
        var userId = jwt.getSubject();

        var vaultIds = processRepo.findVaultIdsByParticipant(userId)
                .distinct()
                .collect(Collectors.toList());

        if (vaultIds.isEmpty()) {
            return List.of();
        }

        return vaultRepo.findAllInList(vaultIds)
                .filter(v -> !v.isArchived())
                .map(VaultResource.VaultDto::fromEntity)
                .toList();
    }
}
