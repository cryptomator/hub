package org.cryptomator.hub.api;

import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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
				return new UserDto(authority.id, authority.name, user.pictureUrl, user.email, null);
			} else if (authority instanceof Group) {
				return new GroupDto(authority.id, authority.name);
			} else {
				throw new IllegalStateException("authority is not of type user or group");
			}
		}).toList();
	}

}