package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Group;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/groups")
@Produces(MediaType.TEXT_PLAIN)
public class GroupsResource {

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all groups")
	public List<GroupDto> getAll() {
		return Group.findAll().<Group>stream().map(GroupDto::fromEntity).toList();
	}

}