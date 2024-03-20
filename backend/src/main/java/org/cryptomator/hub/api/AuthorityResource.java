package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.AuthorityRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.util.List;

@Path("/authorities")
@Produces(MediaType.TEXT_PLAIN)
public class AuthorityResource {

	@Inject
	AuthorityRepository authorityRepo;

	@GET
	@Path("/search")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "search authority by name")
	public List<AuthorityDto> search(@QueryParam("query") @NotBlank String query) {
		return authorityRepo.byName(query).map(AuthorityDto::fromEntity).toList();
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "lists all authorities matching the given ids", description = "lists for each id in the list its corresponding authority. Ignores all id's where an authority cannot be found")
	@APIResponse(responseCode = "200")
	public List<AuthorityDto> getSome(@QueryParam("ids") List<String> authorityIds) {
		return authorityRepo.findAllInList(authorityIds).map(AuthorityDto::fromEntity).toList();
	}

}