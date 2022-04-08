package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.RemoteUserProviderFactory;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.entities.Authority;
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
	SyncerConfig syncerConfig;

	@GET
	@Path("/search")
	@RolesAllowed("vault-owner")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Operation(summary = "search group")
	public List<GroupDto> search(@QueryParam("querry") String querry) {
		return new RemoteUserProviderFactory().get(syncerConfig).searchGroup(querry).map(GroupDto::fromEntity).toList();
	}

	public static final class GroupDto extends AuthorityDto {

		GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name) {
			super(id, "group", name); // TODO keep string "user"?
		}

		public static GroupDto fromEntity(Group group) {
			return new GroupDto(group.id, group.name);
		}

	}

}