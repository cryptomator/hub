package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
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
	@Operation(summary = "search authority")
	public List<AuthorityDto> search(@QueryParam("query") @NotBlank String query) {
		return Authority.byName(query).map(authority -> {
			if (authority instanceof User user) {
				return UserDto.justPublicInfo(user);
			} else if (authority instanceof Group group) {
				return GroupDto.fromEntity(group);
			} else {
				throw new IllegalStateException("authority is not of type user or group");
			}
		}).toList();
	}

}