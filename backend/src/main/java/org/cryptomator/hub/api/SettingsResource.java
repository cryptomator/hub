package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.Settings;
import org.cryptomator.hub.entities.events.EventLogger;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.Set;
import java.util.stream.Collectors;

@Path("/settings")
public class SettingsResource {

	@Inject
	EventLogger eventLogger;

	@Inject
	Settings.Repository settingsRepo;

	@Inject
	JsonWebToken jwt;

	@GET
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "get the billing information")
	@APIResponse(responseCode = "200")
	@Transactional
	public SettingsDto get() {
		return SettingsDto.fromEntity(settingsRepo.get());
	}

	@PUT
	@RolesAllowed("admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "update settings")
	@APIResponse(responseCode = "204", description = "token set")
	@APIResponse(responseCode = "400", description = "invalid settings")
	@APIResponse(responseCode = "403", description = "only admins are allowed to update settings")
	@Transactional
	public Response put(@NotNull @Valid SettingsDto dto) {
		var settings = settingsRepo.get();
		var oldWotIdVerifyLen = settings.getWotIdVerifyLen();
		var oldWotMaxDepth = settings.getWotMaxDepth();
		var oldEmergencyCouncilMemberIds = settings.getEmergencyCouncilMemberIds();
		var oldRequiredEmergencyKeyShares = settings.getDefaultRequiredEmergencyKeyShares();
		var oldMinMembers = settings.getDefaultMinMembers();
		var oldAllowChoosingEmergencyCouncil = settings.isAllowChoosingEmergencyCouncil();
		var oldEmergencyAccessEnabled = settings.isEmergencyAccessEnabled();
		settings.setWotMaxDepth(dto.wotMaxDepth);
		settings.setWotIdVerifyLen(dto.wotIdVerifyLen);
		settings.setDefaultRequiredEmergencyKeyShares(dto.defaultRequiredEmergencyKeyShares);
		settings.setDefaultMinMembers(dto.defaultMinMembers);
		settings.setAllowChoosingEmergencyCouncil(dto.allowChoosingEmergencyCouncil);
		settings.setEmergencyCouncilMemberIds(dto.emergencyCouncilMemberIds);
		settings.setEmergencyAccessEnabled(dto.enableEmergencyAccess);
		settingsRepo.persist(settings);
		if (oldWotMaxDepth != dto.wotMaxDepth || oldWotIdVerifyLen != dto.wotIdVerifyLen) {
			eventLogger.logWotSettingUpdated(jwt.getSubject(), dto.wotIdVerifyLen, dto.wotMaxDepth);
		}
		if (!oldEmergencyCouncilMemberIds.containsAll(dto.emergencyCouncilMemberIds) 
			|| !dto.emergencyCouncilMemberIds.containsAll(oldEmergencyCouncilMemberIds)
			|| oldRequiredEmergencyKeyShares != dto.defaultRequiredEmergencyKeyShares 
			|| oldAllowChoosingEmergencyCouncil != dto.allowChoosingEmergencyCouncil 
			|| oldMinMembers != dto.defaultMinMembers 
			|| oldEmergencyAccessEnabled != dto.enableEmergencyAccess) {
			var councilMemberIds = "[" + dto.emergencyCouncilMemberIds.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]";
			eventLogger.logEmergencyAccessSettingsUpdated(jwt.getSubject(), dto.enableEmergencyAccess, councilMemberIds, dto.defaultRequiredEmergencyKeyShares, dto.defaultMinMembers, dto.allowChoosingEmergencyCouncil);
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	public record SettingsDto(@JsonProperty("hubId") String hubId,
							  @JsonProperty("wotMaxDepth") @Min(0) @Max(9) int wotMaxDepth,
							  @JsonProperty("wotIdVerifyLen") @Min(0) int wotIdVerifyLen,
							  @JsonProperty("enableEmergencyAccess") boolean enableEmergencyAccess,
							  @JsonProperty("defaultRequiredEmergencyKeyShares") @Min(0) int defaultRequiredEmergencyKeyShares,
							  @JsonProperty("defaultMinMembers") @Min(0) int defaultMinMembers,
							  @JsonProperty("allowChoosingEmergencyCouncil") boolean allowChoosingEmergencyCouncil,
							  @JsonProperty("emergencyCouncilMemberIds") @NotNull Set<String> emergencyCouncilMemberIds) {

		public static SettingsDto fromEntity(Settings entity) {
			return new SettingsDto(entity.getHubId(), entity.getWotMaxDepth(), entity.getWotIdVerifyLen(), entity.isEmergencyAccessEnabled(), entity.getDefaultRequiredEmergencyKeyShares(), entity.getDefaultMinMembers(), entity.isAllowChoosingEmergencyCouncil(), entity.getEmergencyCouncilMemberIds());
		}

	}

}
