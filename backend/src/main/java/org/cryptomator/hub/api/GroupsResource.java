package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Path("/groups")
public class GroupsResource {

	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all groups")
	public List<GroupDto> getAll() {
		List<Group> groups = groupRepo.findAll().list();
		return groups.stream().map(group -> GroupDto.fromEntity(group, groupRepo.countMembers(group.getId()))).toList();
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