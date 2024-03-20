package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.GroupRepository;
import org.cryptomator.hub.entities.UserRepository;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/groups")
public class GroupsResource {

	@Inject
	UserRepository userRepo;
	@Inject
	GroupRepository groupRepo;

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all groups")
	public List<GroupDto> getAll() {
		return groupRepo.findAll().stream().map(GroupDto::fromEntity).toList();
	}

	@GET
	@Path("/{groupId}/effective-members")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all effective group members")
	public List<UserDto> getEffectiveMembers(@PathParam("groupId") @ValidId String groupId) {
		return userRepo.getEffectiveGroupUsers(groupId).map(UserDto::justPublicInfo).toList();
	}

}