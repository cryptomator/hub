package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.LegacyAccessToken;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultKeyRetrievedEvent;
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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/vaults")
public class VaultResource {

	@Inject
	EventLogger eventLogger;

	@Inject
	AccessToken.Repository accessTokenRepo;
	@Inject
	Group.Repository groupRepo;
	@Inject
	User.Repository userRepo;
	@Inject
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;
	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Inject
	@Deprecated(since = "1.3.0", forRemoval = true)
	LegacyAccessToken.Repository legacyAccessTokenRepo;
	@Inject
	Vault.Repository vaultRepo;
	@Inject
	VaultAccess.Repository vaultAccessRepo;

	@Inject
	JsonWebToken jwt;

	@Inject
	SecurityIdentity identity;

	@Inject
	LicenseHolder license;

	@Context
	HttpServerRequest request;

	@GET
	@Path("/accessible")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all accessible vaults", description = "list all vaults that have been shared with the currently logged in user or a group in wich this user is")
	public List<VaultDto> getAccessible(@Nullable @QueryParam("role") VaultAccess.Role role) {
		var currentUserId = jwt.getSubject();
		final Stream<Vault> resultStream;
		if (role == null) {
			resultStream = vaultRepo.findAccessibleByUser(currentUserId);
		} else {
			resultStream = vaultRepo.findAccessibleByUser(currentUserId, role);
		}
		return resultStream.map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/recoverable")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all recoverable vaults", description = "list all vaults that can be recovered by the currently logged in user")
	public List<VaultDto> getRecoverable() {
		var currentUserId = jwt.getSubject();
		final Stream<Vault> resultStream = vaultRepo.findRecoverable(currentUserId);
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
		return vaultRepo.findAllInList(vaultIds).map(VaultDto::fromEntity).toList();
	}

	@GET
	@Path("/all")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "list all vaults", description = "list all vaults in the system")
	public List<VaultDto> getAllVaults() {
		return vaultRepo.findAll().stream().map(VaultDto::fromEntity).toList();
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
		return vaultAccessRepo.forVault(vaultId).map(access -> switch (access.getAuthority()) {
			case User u -> MemberDto.fromEntity(u, access.getRole());
			case Group g -> MemberDto.fromEntity(g, access.getRole());
			default -> throw new IllegalStateException();
		}).toList();
	}

	@PUT
	@Path("/{vaultId}/users/{userId}")
	@RolesAllowed("user")
	@VaultRole(value = VaultAccess.Role.OWNER, bypassForEmergencyAccess = true) // may throw 403
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds a user to this vault or updates her role")
	@Parameter(name = "role", in = ParameterIn.QUERY, description = "the role to grant to this user (defaults to MEMBER)")
	@APIResponse(responseCode = "200", description = "user's role updated")
	@APIResponse(responseCode = "201", description = "user added")
	@APIResponse(responseCode = "402", description = "license is expired or licensed seats would be exceeded after the operation")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	@APIResponse(responseCode = "404", description = "user not found")
	@ActiveLicense
	public Response addUser(@PathParam("vaultId") UUID vaultId, @PathParam("userId") @ValidId String userId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		var vault = vaultRepo.findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		var user = userRepo.findByIdOptional(userId).orElseThrow(NotFoundException::new);
		var usedSeats = effectiveVaultAccessRepo.countSeatOccupyingUsers();
		if (usedSeats < license.getSeats() // free seats available
				|| effectiveVaultAccessRepo.isUserOccupyingSeat(userId)) { // or user already sitting
			return addAuthority(vault, user, role);
		} else {
			throw new PaymentRequiredException("License seats exceeded. Cannot add more users.");
		}
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
	@APIResponse(responseCode = "402", description = "license is expired or licensed seats would be exceeded after the operation")
	@APIResponse(responseCode = "403", description = "not a vault owner")
	@APIResponse(responseCode = "404", description = "group not found")
	@ActiveLicense
	public Response addGroup(@PathParam("vaultId") UUID vaultId, @PathParam("groupId") @ValidId String groupId, @QueryParam("role") @DefaultValue("MEMBER") VaultAccess.Role role) {
		var vault = vaultRepo.findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		var group = groupRepo.findByIdOptional(groupId).orElseThrow(NotFoundException::new);

		//usersInGroup - usersInGroupAndPartOfAtLeastOneVault + usersOfAtLeastOneVault
		if (userRepo.countEffectiveGroupUsers(groupId) - effectiveVaultAccessRepo.countSeatOccupyingUsersOfGroup(groupId) + effectiveVaultAccessRepo.countSeatOccupyingUsers() > license.getSeats()) {
			throw new PaymentRequiredException("Adding this group would exceed available license seats.");
		}

		return addAuthority(vault, group, role);
	}

	private Response addAuthority(Vault vault, Authority authority, VaultAccess.Role role) {
		var id = new VaultAccess.Id(vault.getId(), authority.getId());
		var existingAccess = vaultAccessRepo.findByIdOptional(id);
		if (existingAccess.isPresent()) {
			var access = existingAccess.get();
			access.setRole(role);
			vaultAccessRepo.persist(access);
			eventLogger.logVaultMemberUpdated(jwt.getSubject(), vault.getId(), authority.getId(), role);
			return Response.ok().build();
		} else {
			var access = new VaultAccess();
			access.setVault(vault);
			access.setAuthority(authority);
			access.setRole(role);
			vaultAccessRepo.persist(access);
			eventLogger.logVaultMemberAdded(jwt.getSubject(), vault.getId(), authority.getId(), role);
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
		if (vaultAccessRepo.deleteById(new VaultAccess.Id(vaultId, authorityId))) {
			eventLogger.logVaultMemberRemoved(jwt.getSubject(), vaultId, authorityId);
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
		return userRepo.findRequiringAccessGrant(vaultId).map(UserDto::justPublicInfo).toList();
	}

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
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
		var vault = vaultRepo.findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.isArchived()) {
			throw new GoneException("Vault is archived.");
		}

		var accessTokenSeats = effectiveVaultAccessRepo.countSeatOccupyingUsersWithAccessToken();
		if (accessTokenSeats > license.getSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}
		var ipAddress = request.remoteAddress().hostAddress();
		try {
			var access = legacyAccessTokenRepo.unlock(vaultId, deviceId, jwt.getSubject());
			eventLogger.logVaultKeyRetrieved(jwt.getSubject(), vaultId, VaultKeyRetrievedEvent.Result.SUCCESS, ipAddress, deviceId);
			var subscriptionStateHeaderName = "Hub-Subscription-State";
			var subscriptionStateHeaderValue = license.isSet() ? "ACTIVE" : "INACTIVE"; // license expiration is not checked here, because it is checked in the ActiveLicense filter
			return Response.ok(access.getJwe()).header(subscriptionStateHeaderName, subscriptionStateHeaderValue).build();
		} catch (NoResultException e) {
			eventLogger.logVaultKeyRetrieved(jwt.getSubject(), vaultId, VaultKeyRetrievedEvent.Result.UNAUTHORIZED, ipAddress, deviceId);
			throw new ForbiddenException("Access to this device not granted.");
		}
	}

	@GET
	@Path("/{vaultId}/access-token")
	@RolesAllowed("user")
	@VaultRole({VaultAccess.Role.MEMBER, VaultAccess.Role.OWNER}) // may throw 403
	@Transactional
	@Operation(summary = "get the user-specific vault key", description = "retrieves a jwe containing the vault key, encrypted for the current user")
	@APIResponse(responseCode = "200", content = {@Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))})
	@APIResponse(responseCode = "402", description = "license expired or number of effective vault users that have a token exceeds available license seats")
	@APIResponse(responseCode = "403", description = "not a vault member")
	@APIResponse(responseCode = "404", description = "unknown vault")
	@APIResponse(responseCode = "410", description = "Vault is archived. Only returned if evenIfArchived query param is false or not set, otherwise the archived flag is ignored")
	@APIResponse(responseCode = "449", description = "User account not yet initialized. Retry after setting up user")
	@ActiveLicense // may throw 402
	public Response unlock(@PathParam("vaultId") UUID vaultId, @QueryParam("evenIfArchived") @DefaultValue("false") boolean ignoreArchived) {
		var vault = vaultRepo.findById(vaultId); // should always be found, since @VaultRole filter would have triggered
		if (vault.isArchived() && !ignoreArchived) {
			throw new GoneException("Vault is archived.");
		}

		var accessTokenSeats = effectiveVaultAccessRepo.countSeatOccupyingUsersWithAccessToken();
		if (accessTokenSeats > license.getSeats()) {
			throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
		}

		var user = userRepo.findById(jwt.getSubject());
		if (user.getEcdhPublicKey() == null) {
			throw new ActionRequiredException("User account not initialized.");
		}
		var ipAddress = request.remoteAddress().hostAddress();
		var deviceId = request.getHeader("Hub-Device-ID");
		var access = accessTokenRepo.unlock(vaultId, jwt.getSubject());
		if (access != null) {
			eventLogger.logVaultKeyRetrieved(jwt.getSubject(), vaultId, VaultKeyRetrievedEvent.Result.SUCCESS, ipAddress, deviceId);
			var subscriptionStateHeaderName = "Hub-Subscription-State";
			var subscriptionStateHeaderValue = license.isSet() ? "ACTIVE" : "INACTIVE"; // license expiration is not checked here, because it is checked in the ActiveLicense filter
			return Response.ok(access.getVaultKey(), MediaType.TEXT_PLAIN_TYPE).header(subscriptionStateHeaderName, subscriptionStateHeaderValue).build();
		} else if (vaultRepo.findById(vaultId) == null) {
			throw new NotFoundException("No such vault.");
		} else {
			eventLogger.logVaultKeyRetrieved(jwt.getSubject(), vaultId, VaultKeyRetrievedEvent.Result.UNAUTHORIZED, ipAddress, deviceId);
			throw new ForbiddenException("Access to this vault not granted.");
		}
	}

	@POST
	@Path("/{vaultId}/access-tokens")
	@RolesAllowed("user")
	@VaultRole(value = VaultAccess.Role.OWNER, bypassForEmergencyAccess = true) // may throw 403
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds user-specific vault keys", description = "Stores one or more user-vaultkey-tuples, as defined in the request body ({user1: token1, user2: token2, ...}).")
	@APIResponse(responseCode = "200", description = "all keys stored")
	@APIResponse(responseCode = "402", description = "number of users granted access exceeds available license seats")
	@APIResponse(responseCode = "403", description = "not a vault owner or emergency access council member")
	@APIResponse(responseCode = "404", description = "at least one user has not been found")
	public Response grantAccess(@PathParam("vaultId") UUID vaultId, @NotEmpty Map<String, String> tokens) {
		var vault = vaultRepo.findById(vaultId); // should always be found, since @VaultRole filter would have triggered

		// check number of available seats
		long occupiedSeats = effectiveVaultAccessRepo.countSeatOccupyingUsers();
		long usersWithoutSeat = tokens.size() - effectiveVaultAccessRepo.countSeatsOccupiedByUsers(tokens.keySet().stream().toList());

		if (occupiedSeats + usersWithoutSeat > license.getSeats()) {
			throw new PaymentRequiredException("Number of effective vault users greater than or equal to the available license seats");
		}

		for (var entry : tokens.entrySet()) {
			var userId = entry.getKey();
			var token = accessTokenRepo.findById(new AccessToken.AccessId(userId, vaultId));
			if (token == null) {
				token = new AccessToken();
				token.setVault(vault);
				token.setUser(userRepo.findByIdOptional(userId).orElseThrow(NotFoundException::new));
			}
			token.setVaultKey(entry.getValue());
			accessTokenRepo.persist(token);
			eventLogger.logVaultAccessGranted(jwt.getSubject(), vaultId, userId);
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
	@APIResponse(responseCode = "403", description = "requesting user is neither a vault member nor has the admin role")
	public VaultDto get(@PathParam("vaultId") UUID vaultId) {
		Vault vault = vaultRepo.findByIdOptional(vaultId).orElseThrow(NotFoundException::new);
		if (vault.getEffectiveMembers().stream().noneMatch(u -> u.getId().equals(jwt.getSubject())) && !identity.getRoles().contains("admin")) {
			throw new ForbiddenException("Requesting user is not a member of the vault");
		}
		return VaultDto.fromEntity(vault);
	}

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user") // general authentication. VaultRole filter will check for specific access rights
	@VaultRole(value = VaultAccess.Role.OWNER, onMissingVault = VaultRole.OnMissingVault.REQUIRE_REALM_ROLE, realmRole = "create-vaults", bypassForEmergencyAccess = true)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates or updates a vault",
			description = "Creates or updates a vault with the given vault id. The creationTime in the vaultDto is always ignored. On creation, the current server time is used and the archived field is ignored. On update, only the name, description, and archived fields are considered.")
	@APIResponse(responseCode = "200", description = "existing vault updated")
	@APIResponse(responseCode = "201", description = "new vault created")
	@APIResponse(responseCode = "402", description = "number of licensed seats is exceeded")
	public Response createOrUpdate(@PathParam("vaultId") UUID vaultId, @Valid @NotNull VaultDto vaultDto) {
		User currentUser = userRepo.findById(jwt.getSubject());
		Optional<Vault> existingVault = vaultRepo.findByIdOptional(vaultId);
		final Vault vault;
		if (existingVault.isPresent()) {
			// load existing vault:
			vault = existingVault.get();
		} else {
			//if license is exceeded block vault creation, independent if the user is already sitting
			var usedSeats = effectiveVaultAccessRepo.countSeatOccupyingUsers();
			if (usedSeats > license.getSeats()) {
				throw new PaymentRequiredException("Number of effective vault users exceeds available license seats");
			}
			// create new vault:
			vault = new Vault();
			vault.setId(vaultDto.id);
			vault.setCreationTime(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		}
		// set regardless of whether vault is new or existing:
		vault.setName(vaultDto.name);
		vault.setDescription(vaultDto.description);
		vault.setArchived(existingVault.isEmpty() ? false : vaultDto.archived);
		var oldEmergencyKeyShares = vault.getEmergencyKeyShares().values();
		vault.setRequiredEmergencyKeyShares(vaultDto.requiredEmergencyKeyShares);
		vault.setEmergencyKeyShares(vaultDto.emergencyKeyShares);

		vaultRepo.persistAndFlush(vault); // trigger PersistenceException before we continue with

		// does this request update emergency key shares?
		if (!oldEmergencyKeyShares.containsAll(vaultDto.emergencyKeyShares.values())) {
			var emergencyAccessCouncilMembers = String.join("\", \"", vaultDto.emergencyKeyShares.keySet());
			var settings = """
					{ "requiredEmergencyKeyShares": %d, "emergencyCouncilMemberIds": ["%s"] }
					""".formatted(vaultDto.requiredEmergencyKeyShares, emergencyAccessCouncilMembers);
			eventLogger.logEmergencyAccessSetup(vault.getId(), currentUser.getId(), settings, request.remoteAddress().hostAddress());
		}

		if (existingVault.isEmpty()) {
			eventLogger.logVaultCreated(currentUser.getId(), vault.getId(), vault.getName(), vault.getDescription());
			var access = new VaultAccess();
			access.setVault(vault);
			access.setAuthority(currentUser);
			access.setRole(VaultAccess.Role.OWNER);
			vaultAccessRepo.persist(access);
			eventLogger.logVaultMemberAdded(currentUser.getId(), vaultId, currentUser.getId(), VaultAccess.Role.OWNER);
			return Response.created(URI.create(".")).contentLocation(URI.create(".")).entity(VaultDto.fromEntity(vault)).type(MediaType.APPLICATION_JSON).build();
		} else {
			eventLogger.logVaultUpdated(currentUser.getId(), vault.getId(), vault.getName(), vault.getDescription(), vault.isArchived());
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
		User currentUser = userRepo.findById(jwt.getSubject());
		Vault vault = vaultRepo.findByIdOptional(vaultId).orElseThrow(NotFoundException::new);

		// if vault.authenticationPublicKey no longer exists, this vault has already been claimed by a different user
		var authPubKey = vault.getAuthenticationPublicKeyOptional().orElseThrow(() -> new ClientErrorException(Response.Status.CONFLICT));

		try {
			var verifier = JWT.require(Algorithm.ECDSA384(authPubKey))
					.acceptLeeway(30)
					.withClaimPresence("nbf")
					.withClaimPresence("exp")
					.withSubject(currentUser.getId())
					.withClaim("vaultId", vaultId.toString().toLowerCase())
					.build();
			verifier.verify(proof);
		} catch (JWTVerificationException e) {
			throw new BadRequestException("Invalid proof of ownership", e);
		}

		Optional<VaultAccess> existingAccess = vaultAccessRepo.findByIdOptional(new VaultAccess.Id(vaultId, currentUser.getId()));
		if (existingAccess.isPresent()) {
			var access = existingAccess.get();
			access.setRole(VaultAccess.Role.OWNER);
			vaultAccessRepo.persist(access);
			eventLogger.logVaultMemberUpdated(currentUser.getId(), vaultId, currentUser.getId(), VaultAccess.Role.OWNER);
		} else {
			var access = new VaultAccess();
			access.setVault(vault);
			access.setAuthority(currentUser);
			access.setRole(VaultAccess.Role.OWNER);
			vaultAccessRepo.persist(access);
			eventLogger.logVaultMemberAdded(currentUser.getId(), vaultId, currentUser.getId(), VaultAccess.Role.OWNER);
		}

		vault.setSalt(null);
		vault.setIterations(null);
		vault.setMasterkey(null);
		vault.setAuthenticationPrivateKey(null);
		vault.setAuthenticationPublicKey(null);
		vaultRepo.persist(vault);

		eventLogger.logVaultOwnershipClaimed(currentUser.getId(), vaultId);
		return Response.ok(VaultDto.fromEntity(vault), MediaType.APPLICATION_JSON).build();
	}


	public record VaultDto(@JsonProperty("id") UUID id,
						   @JsonProperty("name") @NoHtmlOrScriptChars @NotBlank String name,
						   @JsonProperty("creationTime") Instant creationTime, @JsonProperty("description") @NoHtmlOrScriptChars String description,
						   @JsonProperty("archived") boolean archived,
						   @JsonProperty("requiredEmergencyKeyShares") @Min(0) int requiredEmergencyKeyShares,
						   @JsonProperty("emergencyKeyShares") Map<String, String> emergencyKeyShares,
						   // Legacy properties ("Vault Admin Password"):
						   @JsonProperty("masterkey") @OnlyBase64Chars String masterkey, @JsonProperty("iterations") Integer iterations,
						   @JsonProperty("salt") @OnlyBase64Chars String salt,
						   @JsonProperty("authPublicKey") @OnlyBase64Chars String authPublicKey, @JsonProperty("authPrivateKey") @OnlyBase64Chars String authPrivateKey
	) {

		public static VaultDto fromEntity(Vault entity) {
			return new VaultDto(entity.getId(), entity.getName(), entity.getCreationTime().truncatedTo(ChronoUnit.MILLIS), entity.getDescription(), entity.isArchived(), entity.getRequiredEmergencyKeyShares(), entity.getEmergencyKeyShares(), entity.getMasterkey(), entity.getIterations(), entity.getSalt(), entity.getAuthenticationPublicKey(), entity.getAuthenticationPrivateKey());
		}

	}
}
