package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
import org.cryptomator.hub.keycloak.KeycloakAdminService;
import org.cryptomator.hub.validation.ValidId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

@Path("/groups")
public class GroupsResource {

	private static final Logger LOG = Logger.getLogger(GroupsResource.class.getName());

	@Inject
	User.Repository userRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	VaultAccess.Repository vaultAccessRepo;
	@Inject
	KeycloakAdminService keycloakAdminService;

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
	public List<UserDto.UserDtoWithName> getEffectiveMembers(@PathParam("groupId") @ValidId String groupId) {
		return getMembersWithNames(groupId);
	}

	@POST
	@Path("/{groupId}/members/{userId}")
	@RolesAllowed("admin")
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
	@RolesAllowed("admin")
	@Transactional
	@Operation(summary = "remove a user from a group")
	@APIResponse(responseCode = "204", description = "user removed from group")
	@APIResponse(responseCode = "404", description = "group or user not found")
	public Response removeMember(@PathParam("groupId") @ValidId String groupId, @PathParam("userId") @ValidId String userId) {
		keycloakAdminService.removeUserFromGroup(groupId, userId);
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
			var groupRepresentation = keycloakAdminService.createGroup(dto.name(), dto.pictureUrl());

			Group group = groupRepo.findById(groupRepresentation.getId());
			if (group == null) {
				throw new RuntimeException("Group was created in Keycloak but not found in database after sync");
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
	public GroupDtoWithDetails getGroup(@PathParam("groupId") @ValidId String groupId) {
		Group group = groupRepo.findById(groupId);
		if (group == null) {
			throw new jakarta.ws.rs.NotFoundException("Group not found: " + groupId);
		}

		List<UserDto.UserDtoWithName> members = getMembersWithNames(groupId);

		List<VaultResource.VaultDtoWithRole> vaults = vaultAccessRepo.findByAuthority(groupId)
				.map(va -> VaultResource.VaultDtoWithRole.from(va.getVault(), va.getRole()))
				.toList();

		return GroupDtoWithDetails.from(group, members, vaults);
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
		keycloakAdminService.updateGroup(groupId, dto.name(), dto.pictureUrl());

		Group group = groupRepo.findById(groupId);
		if (group == null) {
			throw new jakarta.ws.rs.NotFoundException("Group not found after update: " + groupId);
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
		keycloakAdminService.deleteGroup(groupId);
		return Response.noContent().build();
	}

	private List<UserDto.UserDtoWithName> getMembersWithNames(String groupId) {
		return userRepo.getEffectiveGroupUsers(groupId)
				.map(user -> {
					String firstName = null;
					String lastName = null;
					try {
						var keycloakUser = keycloakAdminService.getUser(user.getId());
						firstName = keycloakUser.getFirstName();
						lastName = keycloakUser.getLastName();
					} catch (Exception e) {
						LOG.fine("Could not fetch Keycloak user data for " + user.getId());
					}
					return UserDto.UserDtoWithName.from(user, firstName, lastName);
				})
				.toList();
	}

	public record CreateGroupDto(
			@JsonProperty("name") @NotNull String name,
			@JsonProperty("pictureUrl") String pictureUrl
	) {
	}

	public record UpdateGroupDto(
			@JsonProperty("name") String name,
			@JsonProperty("pictureUrl") String pictureUrl
	) {
	}

	public record GroupDtoWithDetails(
			@JsonProperty("id") String id,
			@JsonProperty("name") String name,
			@JsonProperty("pictureUrl") String pictureUrl,
			@JsonProperty("members") List<UserDto.UserDtoWithName> members,
			@JsonProperty("vaults") List<VaultResource.VaultDtoWithRole> vaults
	) {
		public static GroupDtoWithDetails from(Group group, List<UserDto.UserDtoWithName> members, List<VaultResource.VaultDtoWithRole> vaults) {
			return new GroupDtoWithDetails(
					group.getId(),
					group.getName(),
					group.getPictureUrl(),
					members,
					vaults
			);
		}
	}
}