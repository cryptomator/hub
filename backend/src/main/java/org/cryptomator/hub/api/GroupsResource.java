package org.cryptomator.hub.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.keycloak.KeycloakAdminService;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

@Path("/groups")
public class GroupsResource {

	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	KeycloakAdminService keycloakAdminService;

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

	@POST
	@Path("/{groupId}/members/{userId}")
	@RolesAllowed("user")
	@Transactional
	@Operation(summary = "add a user to a group")
	@APIResponse(responseCode = "204", description = "user added to group")
	@APIResponse(responseCode = "404", description = "group or user not found")
	public Response addMember(@PathParam("groupId") @ValidId String groupId, @PathParam("userId") @ValidId String userId) {
		keycloakAdminService.addUserToGroup(groupId, userId);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{groupId}/members/{userId}")
	@RolesAllowed("user")
	@Transactional
	@Operation(summary = "remove a user from a group")
	@APIResponse(responseCode = "204", description = "user removed from group")
	@APIResponse(responseCode = "404", description = "group or user not found")
	public Response removeMember(@PathParam("groupId") @ValidId String groupId, @PathParam("userId") @ValidId String userId) {
		keycloakAdminService.removeUserFromGroup(groupId, userId);
		return Response.noContent().build();
	}

}