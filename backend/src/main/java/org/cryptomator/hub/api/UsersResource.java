package org.cryptomator.hub.api;

import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.entities.events.VaultAccessGrantedEvent;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.NoCache;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	AccessToken.Repository accessTokenRepo;
	@Inject
	EventLogger eventLogger;
	@Inject
	User.Repository userRepo;
	@Inject
	Device.Repository deviceRepo;
	@Inject
	Vault.Repository vaultRepo;

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "update the logged-in user")
	@APIResponse(responseCode = "201", description = "user created or updated")
	public Response putMe(@Nullable @Valid UserDto dto) {
		var userId = jwt.getSubject();
		User user = userRepo.findById(userId);
		if (user == null) {
			user = new User();
			user.setId(userId);
		}
		user.setName(jwt.getName());
		user.setPictureUrl(jwt.getClaim("picture"));
		user.setEmail(jwt.getClaim("email"));
		if (dto != null) {
			user.setPublicKey(dto.publicKey);
			user.setPrivateKey(dto.privateKey);
			user.setSetupCode(dto.setupCode);
		}
		userRepo.persist(user);
		return Response.created(URI.create(".")).build();
	}

	@POST
	@Path("/me/access-tokens")
	@RolesAllowed("user")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "adds/updates user-specific vault keys", description = "Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).")
	@APIResponse(responseCode = "200", description = "all keys stored")
	public Response updateMyAccessTokens(@NotNull Map<UUID, String> tokens) {
		var user = userRepo.findById(jwt.getSubject());
		for (var entry : tokens.entrySet()) {
			var vault = vaultRepo.findById(entry.getKey());
			if (vault == null) {
				continue; // skip
			}
			var token = accessTokenRepo.findById(new AccessToken.AccessId(user.getId(), vault.getId()));
			if (token == null) {
				token = new AccessToken();
				token.setVault(vault);
				token.setUser(user);
			}
			token.setVaultKey(entry.getValue());
			accessTokenRepo.persist(token);
			eventLogger.logVaultAccessGranted(user.getId(), vault.getId(), user.getId());
		}
		return Response.ok().build();
	}

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "200", description = "returns the current user")
	@APIResponse(responseCode = "404", description = "no user matching the subject of the JWT passed as Bearer Token")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices) {
		User user = userRepo.findById(jwt.getSubject());
		Function<Device, DeviceResource.DeviceDto> mapDevices = d -> new DeviceResource.DeviceDto(d.getId(), d.getName(), d.getType(), d.getPublickey(), d.getUserPrivateKey(), d.getOwner().getId(), d.getCreationTime().truncatedTo(ChronoUnit.MILLIS));
		var devices = withDevices ? user.devices.stream().map(mapDevices).collect(Collectors.toSet()) : Set.<DeviceResource.DeviceDto>of();
		return new UserDto(user.getId(), user.getName(), user.getPictureUrl(), user.getEmail(), devices, user.getPublicKey(), user.getPrivateKey(), user.getSetupCode());
	}

	@POST
	@Path("/me/reset")
	@RolesAllowed("user")
	@NoCache
	@Transactional
	@Operation(summary = "resets the user account")
	@APIResponse(responseCode = "204", description = "deleted keys, devices and access permissions")
	public Response resetMe() {
		User user = userRepo.findById(jwt.getSubject());
		user.setPublicKey(null);
		user.setPrivateKey(null);
		user.setSetupCode(null);
		userRepo.persist(user);
		deviceRepo.deleteByOwner(user.getId());
		accessTokenRepo.deleteByUser(user.getId());
		return Response.noContent().build();
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		return userRepo.findAll().<User>stream().map(UserDto::justPublicInfo).toList();
	}

}