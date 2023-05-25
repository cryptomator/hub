package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Authority;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.NoCache;

import java.util.List;

@Path("/authorities")
@Produces(MediaType.TEXT_PLAIN)
public class AuthorityResource {

	@GET
	@Path("/search")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "search authority by name")
	public List<AuthorityDto> search(@QueryParam("query") @NotBlank String query) {
		return Authority.byName(query).map(AuthorityDto::fromEntity).toList();
	}

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "list all authorities corresponding to the given ids")
	public List<AuthorityDto> getSome(@QueryParam("ids") List<String> authorityIds) {
		return Authority.findAllInList(authorityIds).map(AuthorityDto::fromEntity).toList();
	}

}