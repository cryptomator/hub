package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.keycloak.KeycloakAuthorityPuller;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.util.List;

@Path("/groups")
public class GroupsResource {

	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	VaultAccess.Repository vaultAccessRepo;
	@Inject
	KeycloakAuthorityPuller keycloakAuthorityPuller;

	@GET
	@Path("/")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all groups")
	public List<GroupDto> getAll() {
		var groups = groupRepo.findAll().list();
		return groups.stream()
				.map(group -> GroupDto.fromEntity(
						group,
						group.getMemberSize(),
						(int) vaultAccessRepo.countByAuthority(group.getId())
				))
				.toList();
	}

	@GET
	@Path("/{groupId}/effective-members")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all effective group members")
	public List<UserDto> getEffectiveMembers(@PathParam("groupId") @ValidId String groupId) {
		return userRepo.getEffectiveGroupUsers(groupId).map(UserDto::justPublicInfo).toList();
	}

	@POST
	@Path("/{groupId}/members/{userId}")
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "add a user to a group")
	@APIResponse(responseCode = "204", description = "user added to group")
	@APIResponse(responseCode = "404", description = "group or user not found")
	public Response addMember(@PathParam("groupId") @ValidId String groupId, @PathParam("userId") @ValidId String userId) {
		keycloakAuthorityPuller.addUserToGroup(groupId, userId);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{groupId}/members/{userId}")
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "remove a user from a group")
	@APIResponse(responseCode = "204", description = "user removed from group")
	@APIResponse(responseCode = "404", description = "group or user not found")
	public Response removeMember(@PathParam("groupId") @ValidId String groupId, @PathParam("userId") @ValidId String userId) {
		keycloakAuthorityPuller.removeUserFromGroup(groupId, userId);
		return Response.noContent().build();
	}

	@POST
	@Path("/")
	@RolesAllowed("admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "create a new group in Keycloak")
	@APIResponse(responseCode = "201", description = "group created")
	@APIResponse(responseCode = "400", description = "invalid input")
	@APIResponse(responseCode = "409", description = "group name already exists")
	public Response createGroup(@Valid @NotNull CreateGroupDto dto) {
		try {
			var groupRepresentation = keycloakAuthorityPuller.createGroup(dto.name(), dto.pictureUrl());

			Group group = groupRepo.findById(groupRepresentation.getId());
			if (group == null) {
				throw new InternalServerErrorException("Group was created in Keycloak but not found in database after sync");
			}

			return Response.created(URI.create("./" + group.getId()))
					.entity(GroupDto.fromEntity(group))
					.build();
		} catch (ClientErrorException e) {
			return Response.status(Response.Status.CONFLICT).build();
		}
	}

	@GET
	@Path("/{groupId}")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get a specific group")
	@APIResponse(responseCode = "200", description = "group found")
	@APIResponse(responseCode = "404", description = "group not found")
	public GroupDto.WithDetails getGroup(@PathParam("groupId") @ValidId String groupId) {
		Group group = groupRepo.findByIdWithEagerDetails(groupId);
		if (group == null) {
			throw new NotFoundException("Group not found: " + groupId);
		}

		List<AuthorityDto> members = group.getMembers().stream().map(AuthorityDto::fromEntity).toList();

		List<VaultResource.VaultDtoWithRole> vaults = group.accessibleVaults.stream()
				.map(va -> VaultResource.VaultDtoWithRole.from(va.getVault(), va.getRole()))
				.toList();

		return GroupDto.fromEntity(group).withDetails(members, vaults);
	}

	@PUT
	@Path("/{groupId}")
	@RolesAllowed("admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "update a group in Keycloak")
	@APIResponse(responseCode = "200", description = "group updated")
	@APIResponse(responseCode = "404", description = "group not found")
	public GroupDto updateGroup(@PathParam("groupId") @ValidId String groupId, @Valid @NotNull UpdateGroupDto dto) {
		keycloakAuthorityPuller.updateGroup(groupId, dto.name(), dto.pictureUrl());

		Group group = groupRepo.findById(groupId);
		if (group == null) {
			throw new NotFoundException("Group not found after update: " + groupId);
		}

		return GroupDto.fromEntity(group);
	}

	@DELETE
	@Path("/{groupId}")
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "delete a group from Keycloak")
	@APIResponse(responseCode = "204", description = "group deleted")
	@APIResponse(responseCode = "404", description = "group not found")
	public Response deleteGroup(@PathParam("groupId") @ValidId String groupId) {
		keycloakAuthorityPuller.deleteGroup(groupId);
		return Response.noContent().build();
	}

	public record CreateGroupDto(
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("pictureUrl") @Size(max = 255) String pictureUrl
	) {

	}

	public record UpdateGroupDto(
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("pictureUrl") @Size(max = 255) String pictureUrl
	) {
	}
}