package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.AuditEventVaultAccessGrant;
import org.cryptomator.hub.entities.AuditEventVaultCreate;
import org.cryptomator.hub.entities.AuditEventVaultKeyRetrieve;
import org.cryptomator.hub.entities.AuditEventVaultMemberAdd;
import org.cryptomator.hub.entities.AuditEventVaultMemberRemove;
import org.cryptomator.hub.entities.AuditEventVaultMemberUpdate;
import org.cryptomator.hub.entities.AuditEventVaultOwnershipClaim;
import org.cryptomator.hub.entities.AuditEventVaultUpdate;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.EffectiveGroupMembership;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.LegacyAccessToken;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.filters.ActiveLicense;
import org.cryptomator.hub.filters.VaultRole;
import org.cryptomator.hub.license.LicenseHolder;
import org.cryptomator.hub.validation.NoHtmlOrScriptChars;
import org.cryptomator.hub.validation.OnlyBase64Chars;
import org.cryptomator.hub.validation.ValidId;
import org.cryptomator.hub.validation.ValidJWS;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Path("/vaults")
public class VaultResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	SecurityIdentity identity;

	@Inject
	LicenseHolder license;

	@GET
	@Path("/accessible")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults that have been shared with the currently logged in user or a group in wich this user is")
	public List<VaultDto> getAccessible(@Nullable @QueryParam("role") VaultAccess.Role role) {
		var currentUserId = jwt.getSubject();
		// TODO refactor to JEP 441 in JDK 21
		final Stream<Vault> resultStream;
		if (role == null) {
			resultStream = Vault.findAccessibleByUser(currentUserId);
		} else {
			resultStream = Vault.findAccessibleByUser(currentUserId, role);
		}
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/some")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all vaults corresponding to the given ids", description = "list for each id in the list its corresponding vault. Ignores all id's where a vault does not exist, ")
	@APIResponse(responseCode = "200")
	public List<VaultDto> getSomeVaults(@QueryParam("ids") List<UUID> vaultIds) {
		return Vault.findAllInList(vaultIds).map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/all")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all vaults", description = "list all vaults in the system")
	public List<VaultDto> getAllVaults() {
		return Vault.findAll().<Vault>stream().map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/{vaultId}/members")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list vault members", description = "list all users or groups that this vault has been shared with directly (not inherited via group membership)")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	public List<MemberDto> getDirectMembers(@PathParam("vaultId") UUID vaultId) {
		return VaultAccess.forVault(vaultId).map(access -> {
			// TODO switch to switch expressions, once we can make Authority sealed
			if (access.authority instanceof User u) {
				return MemberDto.fromEntity(u, access.role);
			} else if (access.authority instanceof Group g) {
				return MemberDto.fromEntity(g, access.role);
			} else {
				throw new IllegalStateException();
			}
		}).toList();
	}

	@PUT
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a user to this vault or updates her role")
	@Parameter(name = "role", in = ParameterIn.QUERY, description = "the role to grant to this user (defaults to MEMBER)")
	@APIResponse(responseCode = "200", description = "user's role updated")
	@APIResponse(responseCode = "201", description = "user added")
	@APIResponse(responseCode = "402", description = "all seats in license used")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	@APIResponse(responseCode = "404", description = "user not found")
	@ActiveLicense
	public Response addUser(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		var vault = Vault.<Vault>findById(vaultId); // // should always be found, since @VaultRole filter would have triggered
		var user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
		if (!EffectiveVaultAccess.isUserOccupyingSeat(userId)) {
			//for new user, we need to check if a license seat is available
			var usedSeats = EffectiveVaultAccess.countSeatOccupyingUsers();
			if (usedSeats >= license.getAvailableSeats()) {
				throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
			}
		}

		return addAuthority(vault, user, role);
	}

	@PUT
	@Path("/{vaultId}/groups/{groupId}")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a group to this vault or updates its role")
	@Parameter(name = "role", in = ParameterIn.QUERY, description = "the role to grant to this group (defaults to MEMBER)")
	@APIResponse(responseCode = "200", description = "group's role updated")
	@APIResponse(responseCode = "201", description = "group added")
	@APIResponse(responseCode = "402", description = "used seats + (number of users in group not occupying a seats) exceeds number of total avaible seats in license")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	@APIResponse(responseCode = "404", description = "group not found")
	@ActiveLicense
	public Response addGroup(@PathParam("vaultId") UUID vaultId, @PathParam("groupId") @ValidId String groupId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		var vault = Vault.<Vault>findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		var group = Group.<Group>findByIdOptional(groupId).orElseThrow(NotFoundException::new);

		//usersInGroup - usersInGroupAndPartOfAtLeastOneVault + usersOfAtLeastOneVault
		if (EffectiveGroupMembership.countEffectiveGroupUsers(groupId) - EffectiveVaultAccess.countSeatOccupyingUsersOfGroup(groupId) + EffectiveVaultAccess.countSeatOccupyingUsers() > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
		}

		return addAuthority(vault, group, role);
	}

	private Response addAuthority(Vault vault, Authority authority, VaultAccess.Role role) {
		var id = new VaultAccess.Id(vault.id, authority.id);
		var existingAccess = VaultAccess.<VaultAccess>findByIdOptional(id);
		if (existingAccess.isPresent()) {
			var access = existingAccess.get();
			access.role = role;
			access.persist();
			AuditEventVaultMemberUpdate.log(jwt.getSubject(), vault.id, authority.id, role);
			return Response.ok().build();
		} else {
			var access = new VaultAccess();
			access.vault = vault;
			access.authority = authority;
			access.role = role;
			access.persist();
			AuditEventVaultMemberAdd.log(jwt.getSubject(), vault.id, authority.id, role);
			return Response.created(URI.create(".")).build();
		}
	}

	@DELETE
	@Path("/{vaultId}/authority/{authorityId}")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "remove a user or group from this vault", description = "revokes the given authority's access rights from this vault. If the given authority is no member, the request is a no-op.")
	@APIResponse(responseCode = "204", description = "authority removed")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	public Response removeAuthority(@PathParam("vaultId") UUID vaultId, @PathParam("authorityId") @ValidId String authorityId) {
		if (VaultAccess.deleteById(new VaultAccess.Id(vaultId, authorityId))) {
			AuditEventVaultMemberRemove.log(jwt.getSubject(), vaultId, authorityId);
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			throw new NotFoundException();
		}
	}

	@GET
	@Path("/{vaultId}/users-requiring-access-grant")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list devices requiring access rights", description = "lists all devices owned by vault members, that don't have a device-specific masterkey yet")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	public List<UserDto> getUsersRequiringAccessGrant(@PathParam("vaultId") UUID vaultId) {
		return User.findRequiringAccessGrant(vaultId).map(UserDto::justPublicInfo).toList();
	}

	@Deprecated(forRemoval = true)
	@GET
	@Path("/{vaultId}/keys/{deviceId}")
	@RolesAllowed("user")
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the device-specific masterkey of a non-archived vault", deprecated = true)
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "number of effective vault users exceeds available license seats")
	@APIResponse(responseCode = "403", description = "not authorized to access this vault")
	@APIResponse(responseCode = "410", description = "Vault is archived")
	@ActiveLicense
	public Response legacyUnlock(@PathParam("vaultId") UUID vaultId, @PathParam("deviceId") @ValidId String deviceId) {
		var vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.archived) {
			throw new GoneException("Vault is archived.");
		}

		var usedSeats = EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var access = LegacyAccessToken.unlock(vaultId, deviceId, jwt.getSubject());
		if (access != null) {
			AuditEventVaultKeyRetrieve.log(jwt.getSubject(), vaultId, AuditEventVaultKeyRetrieve.Result.SUCCESS);
			var subscriptionStateHeaderName = "Hub-Subscription-State";
			var subscriptionStateHeaderValue = license.isSet() ? "ACTIVE" : "INACTIVE"; // license expiration is not checked here, because it is checked in the ActiveLicense filter
			return Response.ok(access.jwe).header(subscriptionStateHeaderName, subscriptionStateHeaderValue).build();
		} else {
			AuditEventVaultKeyRetrieve.log(jwt.getSubject(), vaultId, AuditEventVaultKeyRetrieve.Result.UNAUTHORIZED);
			throw new ForbiddenException("Access to this device not granted.");
		}
	}

	@GET
	@Path("/{vaultId}/access-token")
	@RolesAllowed("user")
	@VaultRole({VaultAccess.Role.MEMBER, VaultAccess.Role.OWNER}) // may throw 403
	@Transactional
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "get the user-specific vault key", description = "retrieves a jwe containing the vault key, encrypted for the current user")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "402", description = "license expired or number of effective vault users that have a token exceeds available license seats")
	@APIResponse(responseCode = "403", description = "not a vault member")
	@APIResponse(responseCode = "404", description = "unknown vault")
	@APIResponse(responseCode = "410", description = "Vault is archived. Only returned if evenIfArchived query param is false or not set, otherwise the archived flag is ignored")
	@APIResponse(responseCode = "449", description = "User account not yet initialized. Retry after setting up user")
	@ActiveLicense // may throw 402
	public Response unlock(@PathParam("vaultId") UUID vaultId, @QueryParam("evenIfArchived") @DefaultValue("false") boolean ignoreArchived) {
		var vault = Vault.<Vault>findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		if (vault.archived && !ignoreArchived) {
			throw new GoneException("Vault is archived.");
		}

		var usedSeats = EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken();
		if (usedSeats > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var user = User.<User>findById(jwt.getSubject());
		if (user.publicKey == null) {
			throw new ActionRequiredException("User account not initialized.");
		}

		var access = AccessToken.unlock(vaultId, jwt.getSubject());
		if (access != null) {
			AuditEventVaultKeyRetrieve.log(jwt.getSubject(), vaultId, AuditEventVaultKeyRetrieve.Result.SUCCESS);
			var subscriptionStateHeaderName = "Hub-Subscription-State";
			var subscriptionStateHeaderValue = license.isSet() ? "ACTIVE" : "INACTIVE"; // license expiration is not checked here, because it is checked in the ActiveLicense filter
			return Response.ok(access.vaultKey).header(subscriptionStateHeaderName, subscriptionStateHeaderValue).build();
		} else if (Vault.findById(vaultId) == null) {
			throw new NotFoundException("No such vault.");
		} else {
			AuditEventVaultKeyRetrieve.log(jwt.getSubject(), vaultId, AuditEventVaultKeyRetrieve.Result.UNAUTHORIZED);
			throw new ForbiddenException("Access to this vault not granted.");
		}
	}

	@POST
	@Path("/{vaultId}/access-tokens")
	@RolesAllowed("user")
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds user-specific vault keys", description = "Stores one or more user-vaultkey-tuples, as defined in the request body ({user1: token1, user2: token2, ...}).")
	@APIResponse(responseCode = "200", description = "all keys stored")
	@APIResponse(responseCode = "402", description = "number of users granted access exceeds available license seats")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	@APIResponse(responseCode = "404", description = "at least one user has not been found")
	@APIResponse(responseCode = "410", description = "vault is archived")
	public Response grantAccess(@PathParam("vaultId") UUID vaultId, @NotEmpty Map<String, String> tokens) {
		var vault = Vault.<Vault>findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		if (vault.archived) {
			throw new GoneException("Vault is archived.");
		}

		// check number of available seats
		long occupiedSeats = EffectiveVaultAccess.countSeatOccupyingUsers();
		long usersWithoutSeat = tokens.size() - EffectiveVaultAccess.countSeatsOccupiedByUsers(tokens.keySet().stream().toList());

		if (occupiedSeats + usersWithoutSeat > license.getAvailableSeats()) {
			throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
		}

		for (var entry : tokens.entrySet()) {
			var userId = entry.getKey();
			var token = AccessToken.<AccessToken>findById(new AccessToken.AccessId(userId, vaultId));
			if (token == null) {
				token = new AccessToken();
				token.vault = vault;
				token.user = User.<User>findByIdOptional(userId).orElseThrow(NotFoundException::new);
			}
			token.vaultKey = entry.getValue();
			token.persist();
			AuditEventVaultAccessGrant.log(jwt.getSubject(), vaultId, userId);
		}
		return Response.ok().build();
	}

	@GET
	@Path("/{vaultId}")
	@RolesAllowed("user")
	// @VaultRole(VaultAccess.Role.MEMBER) // TODO: members and admin may do this...
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a vault")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "requesting user is not member of the vault")
	public VaultDto get(@PathParam("vaultId") UUID vaultId) {
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.effectiveMembers.stream().noneMatch(u -> u.id.equals(jwt.getSubject())) && !identity.getRoles().contains("admin")) {
			throw new ForbiddenException("Requesting user is not a member of the vault");
		}
		return VaultDto.fromEntity(vault);
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@VaultRole(value = VaultAccess.Role.OWNER, onMissingVault = VaultRole.OnMissingVault.PASS)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates or updates a vault",
			description = "Creates or updates a vault with the given vault id. The creationTime in the vaultDto is always ignored. On creation, the current server time is used and the archived field is ignored. On update, only the name, description, and archived fields are considered.")
	@APIResponse(responseCode = "200", description = "existing vault updated")
	@APIResponse(responseCode = "201", description = "new vault created")
	@APIResponse(responseCode = "402", description = "all seats in licence in use during creation of new vault")
	public Response createOrUpdate(@PathParam("vaultId") UUID vaultId, @Valid @NotNull VaultDto vaultDto) {
		User currentUser = User.findById(jwt.getSubject());
		Optional<Vault> existingVault = Vault.findByIdOptional(vaultId);
		final Vault vault;
		if (existingVault.isPresent()) {
			// load existing vault:
			vault = existingVault.get();
		} else {
			if (!EffectiveVaultAccess.isUserOccupyingSeat(currentUser.id)) {
				//for new vaults, we need to check that a licence seat is available if the user does not already have access to a vault.
				var usedSeats = EffectiveVaultAccess.countSeatOccupyingUsers();
				if (usedSeats >= license.getAvailableSeats()) {
					throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
				}
			}
			// create new vault:
			vault = new Vault();
			vault.id = vaultDto.id;
			vault.creationTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		}
		// set regardless of whether vault is new or existing:
		vault.name = vaultDto.name;
		vault.description = vaultDto.description;
		vault.archived = existingVault.isEmpty() ? false : vaultDto.archived;

		vault.metadata = vaultDto.metadata;

		vault.persistAndFlush(); // trigger PersistenceException before we continue with
		if (existingVault.isEmpty()) {
			var access = new VaultAccess();
			access.vault = vault;
			access.authority = currentUser;
			access.role = VaultAccess.Role.OWNER;
			access.persist();
			AuditEventVaultCreate.log(currentUser.id, vault.id, vault.name, vault.description);
			AuditEventVaultMemberAdd.log(currentUser.id, vaultId, currentUser.id, VaultAccess.Role.OWNER);
			return Response.created(URI.create(".")).contentLocation(URI.create(".")).entity(VaultDto.fromEntity(vault)).type(MediaType.APPLICATION_JSON).build();
		} else {
			AuditEventVaultUpdate.log(currentUser.id, vault.id, vault.name, vault.description, vault.archived);
			return Response.ok(VaultDto.fromEntity(vault), MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/{vaultId}/claim-ownership")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	@Operation(summary = "claims ownership of a vault",
			description = "Assigns the OWNER role to the currently logged in user, who proofs this claim by sending a JWT signed with a private key held by users knowing the Vault Admin Password")
	@APIResponse(responseCode = "200", description = "ownership claimed successfully")
	@APIResponse(responseCode = "400", description = "incorrect proof")
	@APIResponse(responseCode = "404", description = "no such vault")
	@APIResponse(responseCode = "409", description = "owned by another user")
	public Response claimOwnership(@PathParam("vaultId") UUID vaultId, @FormParam("proof") @Valid @ValidJWS String proof) {
		User currentUser = User.findById(jwt.getSubject());
		Vault vault = Vault.<Vault>findByIdOptional(vaultId).orElseThrow(NotFoundException::new);

		// if vault.authenticationPublicKey no longer exists, this vault has already been claimed by a different user
		var authPubKey = vault.getAuthenticationPublicKey().orElseThrow(() -> new ClientErrorException(Response.Status.CONFLICT));

		try {
			var verifier = JWT.require(Algorithm.ECDSA384(authPubKey))
					.acceptLeeway(30)
					.withClaimPresence("nbf")
					.withClaimPresence("exp")
					.withSubject(currentUser.id)
					.withClaim("vaultId", vaultId.toString().toLowerCase())
					.build();
			verifier.verify(proof);
		} catch (JWTVerificationException e) {
			throw new BadRequestException("Invalid proof of ownership", e);
		}

		Optional<VaultAccess> existingAccess = VaultAccess.findByIdOptional(new VaultAccess.Id(vaultId, currentUser.id));
		if (existingAccess.isPresent()) {
			var access = existingAccess.get();
			access.role = VaultAccess.Role.OWNER;
			access.persist();
			AuditEventVaultMemberUpdate.log(currentUser.id, vaultId, currentUser.id, VaultAccess.Role.OWNER);
		} else {
			var access = new VaultAccess();
			access.vault = vault;
			access.authority = currentUser;
			access.role = VaultAccess.Role.OWNER;
			access.persist();
			AuditEventVaultMemberAdd.log(currentUser.id, vaultId, currentUser.id, VaultAccess.Role.OWNER);
		}

		vault.salt = null;
		vault.iterations = null;
		vault.masterkey = null;
		vault.authenticationPrivateKey = null;
		vault.authenticationPublicKey = null;
		vault.persist();

		AuditEventVaultOwnershipClaim.log(currentUser.id, vaultId);
		return Response.ok(VaultDto.fromEntity(vault), MediaType.APPLICATION_JSON).build();
	}


	public record VaultDto(@JsonProperty("id") UUID id,
						   @JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
						   @JsonProperty("description") @NoHtmlOrScriptChars String description,
						   @JsonProperty("archived") boolean archived,
						   @JsonProperty("creationTime") Instant creationTime,
						   // Legacy properties ("Vault Admin Password"):
						   @JsonProperty("masterkey") @OnlyBase64Chars String masterkey, @JsonProperty("iterations") Integer iterations,
						   @JsonProperty("salt") @OnlyBase64Chars String salt,
						   @JsonProperty("authPublicKey") @OnlyBase64Chars String authPublicKey, @JsonProperty("authPrivateKey") @OnlyBase64Chars String authPrivateKey
							,@JsonProperty("metadata") @NotNull String metadata
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.id, entity.name, entity.description, entity.archived, entity.creationTime.truncatedTo(ChronoUnit.MILLIS), entity.masterkey, entity.iterations, entity.salt, entity.authenticationPublicKey, entity.authenticationPrivateKey, entity.metadata);
		}

	}
}
