package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.RemoteUserProvider;
import org.cryptomator.hub.entities.Group;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/groups")
@Produces(MediaType.TEXT_PLAIN)
public class GroupResource {

	@Inject
	RemoteUserProvider remoteUserProvider;

	@GET
	@Path("/search")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "search group")
	public List<GroupDto> search(@QueryParam("query") String query) {
		return remoteUserProvider.searchGroup(query).stream().map(GroupDto::fromEntity).toList();
	}

	public static final class GroupDto extends AuthorityDto {

		GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name) {
			super(id, Type.GROUP, name, null);
		}

		public static GroupDto fromEntity(Group group) {
			return new GroupDto(group.id, group.name);
		}

	}

}