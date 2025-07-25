package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.entities.events.*;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Path("/auditlog")
public class AuditLogResource {

	@Inject
	AuditEvent.Repository auditEventRepo;
	@Inject
	LicenseHolder license;

	@GET
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all auditlog entries within a period", description = "list all auditlog entries from a period specified by a start and end date")
	@Parameter(name = "startDate", description = "the start date of the period as ISO 8601 datetime string, inclusive", in = ParameterIn.QUERY)
	@Parameter(name = "endDate", description = "the end date of the period as ISO 8601 datetime string, exclusive", in = ParameterIn.QUERY)
	@Parameter(name = "paginationId", description = "The smallest (asc ordering) or highest (desc ordering) audit entry id, not included in results. Used for pagination. ", in = ParameterIn.QUERY)
	@Parameter(name = "order", description = "The order of the queried table. Determines if most recent (desc) or oldest entries (asc) are considered first. Allowed Values are 'desc' (default) or 'asc'. Used for pagination.", in = ParameterIn.QUERY)
	@Parameter(name = "pageSize", description = "the maximum number of entries to return. Must be between 1 and 100.", in = ParameterIn.QUERY)
	@Parameter(name = "type", description = "the list of type of events to return. Empty list is all events.", in = ParameterIn.QUERY)
	@APIResponse(responseCode = "200", description = "Body contains list of events in the specified time interval")
	@APIResponse(responseCode = "400", description = "startDate or endDate not specified, startDate > endDate, order specified and not in ['asc','desc'], pageSize not in [1 .. 100] or type is not valid")
	@APIResponse(responseCode = "402", description = "Community license used or license expired")
	@APIResponse(responseCode = "403", description = "requesting user does not have admin role")
	public List<AuditEventDto> getAllEvents(@QueryParam("startDate") Instant startDate, @QueryParam("endDate") Instant endDate, @QueryParam("type") List<String> type, @QueryParam("paginationId") Long paginationId, @QueryParam("order") @DefaultValue("desc") String order, @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
		if (!license.isSet() || license.isExpired()) {
			throw new PaymentRequiredException("Community license used or license expired");
		}

		if (startDate == null || endDate == null) {
			throw new BadRequestException("startDate and endDate must be specified");
		} else if (startDate.isAfter(endDate)) {
			throw new BadRequestException("startDate must be before endDate");
		} else if (!(order.equals("desc") || order.equals("asc"))) {
			throw new BadRequestException("order must be either 'asc' or 'desc'");
		} else if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("pageSize must be between 1 and 100");
		} else if (type == null) {
			throw new BadRequestException("type must be specified");
		} else if (!type.isEmpty()) {
			var validTypes = Set.of(DeviceRegisteredEvent.TYPE, DeviceRemovedEvent.TYPE, UserAccountResetEvent.TYPE, UserKeysChangeEvent.TYPE, UserSetupCodeChangeEvent.TYPE,
					SettingWotUpdateEvent.TYPE, SignedWotIdEvent.TYPE, VaultCreatedEvent.TYPE, VaultUpdatedEvent.TYPE, VaultAccessGrantedEvent.TYPE,
					VaultKeyRetrievedEvent.TYPE, VaultMemberAddedEvent.TYPE, VaultMemberRemovedEvent.TYPE, VaultMemberUpdatedEvent.TYPE, VaultOwnershipClaimedEvent.TYPE,
					EmergencyAccessSetupEvent.TYPE, EmergencyAccessSettingsUpdatedEvent.TYPE, EmergencyAccessRecoveryStartedEvent.TYPE, EmergencyAccessRecoveryApprovedEvent.TYPE, EmergencyAccessRecoveryCompletedEvent.TYPE);
			if (!validTypes.containsAll(type)) {
				throw new BadRequestException("Invalid event type provided");
			}
		} else if (paginationId == null) {
			throw new BadRequestException("paginationId must be specified");
		}

		return auditEventRepo.findAllInPeriod(startDate, endDate, type, paginationId, order.equals("asc"), pageSize).map(AuditEventDto::fromEntity).toList();
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ //
			@JsonSubTypes.Type(value = DeviceRegisteredEventDto.class, name = DeviceRegisteredEvent.TYPE), //
			@JsonSubTypes.Type(value = DeviceRemovedEventDto.class, name = DeviceRemovedEvent.TYPE), //
			@JsonSubTypes.Type(value = SettingWotUpdateEventDto.class, name = SettingWotUpdateEvent.TYPE), //
			@JsonSubTypes.Type(value = SignedWotIdEventDto.class, name = SignedWotIdEvent.TYPE), //
			@JsonSubTypes.Type(value = UserAccountResetEventDto.class, name = UserAccountResetEvent.TYPE), //
			@JsonSubTypes.Type(value = UserKeysChangeEventDto.class, name = UserKeysChangeEvent.TYPE), //
			@JsonSubTypes.Type(value = UserSetupCodeChangeEventDto.class, name = UserSetupCodeChangeEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultCreatedEventDto.class, name = VaultCreatedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultUpdatedEventDto.class, name = VaultUpdatedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultAccessGrantedEventDto.class, name = VaultAccessGrantedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultKeyRetrievedEventDto.class, name = VaultKeyRetrievedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultMemberAddedEventDto.class, name = VaultMemberAddedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultMemberRemovedEventDto.class, name = VaultMemberRemovedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultMemberUpdatedEventDto.class, name = VaultMemberUpdatedEvent.TYPE), //
			@JsonSubTypes.Type(value = VaultOwnershipClaimedEventDto.class, name = VaultOwnershipClaimedEvent.TYPE), //
			@JsonSubTypes.Type(value = EmergencyAccessSetupEventDto.class, name = EmergencyAccessSetupEvent.TYPE), //
			@JsonSubTypes.Type(value = EmergencyAccessSettingsUpdatedEventDto.class, name = EmergencyAccessSettingsUpdatedEvent.TYPE), //
			@JsonSubTypes.Type(value = EmergencyAccessRecoveryStartedEventDto.class, name = EmergencyAccessRecoveryStartedEvent.TYPE), //
			@JsonSubTypes.Type(value = EmergencyAccessRecoveryApprovedEventDto.class, name = EmergencyAccessRecoveryApprovedEvent.TYPE), //
			@JsonSubTypes.Type(value = EmergencyAccessRecoveryCompletedEventDto.class, name = EmergencyAccessRecoveryCompletedEvent.TYPE) //
	})
	public interface AuditEventDto {

		@JsonProperty("id")
		long id();

		@JsonProperty("timestamp")
		Instant timestamp();

		static AuditEventDto fromEntity(AuditEvent entity) {
			return switch (entity) {
				case DeviceRegisteredEvent evt -> new DeviceRegisteredEventDto(evt.getId(), evt.getTimestamp(), DeviceRegisteredEvent.TYPE, evt.getRegisteredBy(), evt.getDeviceId(), evt.getDeviceName(), evt.getDeviceType());
				case DeviceRemovedEvent evt -> new DeviceRemovedEventDto(evt.getId(), evt.getTimestamp(), DeviceRemovedEvent.TYPE, evt.getRemovedBy(), evt.getDeviceId());
				case SignedWotIdEvent evt -> new SignedWotIdEventDto(evt.getId(), evt.getTimestamp(), SignedWotIdEvent.TYPE, evt.getUserId(), evt.getSignerId(), evt.getSignerKey(), evt.getSignature());
				case SettingWotUpdateEvent evt -> new SettingWotUpdateEventDto(evt.getId(), evt.getTimestamp(), SettingWotUpdateEvent.TYPE, evt.getUpdatedBy(), evt.getWotMaxDepth(), evt.getWotIdVerifyLen());
				case UserAccountResetEvent evt -> new UserAccountResetEventDto(evt.getId(), evt.getTimestamp(), UserAccountResetEvent.TYPE, evt.getResetBy());
				case UserKeysChangeEvent evt -> new UserKeysChangeEventDto(evt.getId(), evt.getTimestamp(), UserKeysChangeEvent.TYPE, evt.getChangedBy(), evt.getUserName());
				case UserSetupCodeChangeEvent evt -> new UserSetupCodeChangeEventDto(evt.getId(), evt.getTimestamp(), UserSetupCodeChangeEvent.TYPE, evt.getChangedBy());
				case VaultCreatedEvent evt -> new VaultCreatedEventDto(evt.getId(), evt.getTimestamp(), VaultCreatedEvent.TYPE, evt.getCreatedBy(), evt.getVaultId(), evt.getVaultName(), evt.getVaultDescription());
				case VaultUpdatedEvent evt -> new VaultUpdatedEventDto(evt.getId(), evt.getTimestamp(), VaultUpdatedEvent.TYPE, evt.getUpdatedBy(), evt.getVaultId(), evt.getVaultName(), evt.getVaultDescription(), evt.isVaultArchived());
				case VaultAccessGrantedEvent evt -> new VaultAccessGrantedEventDto(evt.getId(), evt.getTimestamp(), VaultAccessGrantedEvent.TYPE, evt.getGrantedBy(), evt.getVaultId(), evt.getAuthorityId());
				case VaultKeyRetrievedEvent evt -> new VaultKeyRetrievedEventDto(evt.getId(), evt.getTimestamp(), VaultKeyRetrievedEvent.TYPE, evt.getRetrievedBy(), evt.getVaultId(), evt.getResult(), evt.getIpAddress(), evt.getDeviceId());
				case VaultMemberAddedEvent evt -> new VaultMemberAddedEventDto(evt.getId(), evt.getTimestamp(), VaultMemberAddedEvent.TYPE, evt.getAddedBy(), evt.getVaultId(), evt.getAuthorityId(), evt.getRole());
				case VaultMemberRemovedEvent evt -> new VaultMemberRemovedEventDto(evt.getId(), evt.getTimestamp(), VaultMemberRemovedEvent.TYPE, evt.getRemovedBy(), evt.getVaultId(), evt.getAuthorityId());
				case VaultMemberUpdatedEvent evt -> new VaultMemberUpdatedEventDto(evt.getId(), evt.getTimestamp(), VaultMemberUpdatedEvent.TYPE, evt.getUpdatedBy(), evt.getVaultId(), evt.getAuthorityId(), evt.getRole());
				case VaultOwnershipClaimedEvent evt -> new VaultOwnershipClaimedEventDto(evt.getId(), evt.getTimestamp(), VaultOwnershipClaimedEvent.TYPE, evt.getClaimedBy(), evt.getVaultId());
				case EmergencyAccessSetupEvent evt -> new EmergencyAccessSetupEventDto(evt.getId(), evt.getTimestamp(), EmergencyAccessSetupEvent.TYPE, evt.getOwnerId(), evt.getSettings(), evt.getIpAddress());
				case EmergencyAccessSettingsUpdatedEvent evt -> new EmergencyAccessSettingsUpdatedEventDto(evt.getId(), evt.getTimestamp(), EmergencyAccessSettingsUpdatedEvent.TYPE, evt.getAdminId(), evt.getCouncilMemberIds(), evt.getRequiredKeyShares(), evt.isAllowChoosingCouncil());
				case EmergencyAccessRecoveryStartedEvent evt -> new EmergencyAccessRecoveryStartedEventDto(evt.getId(), evt.getTimestamp(), EmergencyAccessRecoveryStartedEvent.TYPE, evt.getVaultId(), evt.getProcessId(), evt.getCouncilMemberId(), evt.getProcessType(), evt.getDetails());
				case EmergencyAccessRecoveryApprovedEvent evt -> new EmergencyAccessRecoveryApprovedEventDto(evt.getId(), evt.getTimestamp(), EmergencyAccessRecoveryApprovedEvent.TYPE, evt.getProcessId(), evt.getCouncilMemberId(), evt.getIpAddress());
				case EmergencyAccessRecoveryCompletedEvent evt -> new EmergencyAccessRecoveryCompletedEventDto(evt.getId(), evt.getTimestamp(), EmergencyAccessRecoveryCompletedEvent.TYPE, evt.getProcessId(), evt.getCouncilMemberId());
				default -> throw new UnsupportedOperationException("conversion not implemented for event type " + entity.getClass());
			};
		}
	}

	record DeviceRegisteredEventDto(long id, Instant timestamp, String type, @JsonProperty("registeredBy") String registeredBy, @JsonProperty("deviceId") String deviceId, @JsonProperty("deviceName") String deviceName,
									@JsonProperty("deviceType") Device.Type deviceType) implements AuditEventDto {
	}

	record DeviceRemovedEventDto(long id, Instant timestamp, String type, @JsonProperty("removedBy") String removedBy, @JsonProperty("deviceId") String deviceId) implements AuditEventDto {
	}

	record SignedWotIdEventDto(long id, Instant timestamp, String type, @JsonProperty("userId") String userId, @JsonProperty("signerId") String signerId, @JsonProperty("signerKey") String signerKey,
							   @JsonProperty("signature") String signature) implements AuditEventDto {
	}

	record SettingWotUpdateEventDto(long id, Instant timestamp, String type, @JsonProperty("updatedBy") String updatedBy, @JsonProperty("wotMaxDepth") int wotMaxDepth,
									@JsonProperty("wotIdVerifyLen") int wotIdVerifyLen) implements AuditEventDto {
	}

	record UserAccountResetEventDto(long id, Instant timestamp, String type, @JsonProperty("resetBy") String resetBy) implements AuditEventDto {
	}

	record UserKeysChangeEventDto(long id, Instant timestamp, String type, @JsonProperty("changedBy") String changedBy, @JsonProperty("userName") String userName) implements AuditEventDto {
	}

	record UserSetupCodeChangeEventDto(long id, Instant timestamp, String type, @JsonProperty("changedBy") String changedBy) implements AuditEventDto {
	}

	record VaultCreatedEventDto(long id, Instant timestamp, String type, @JsonProperty("createdBy") String createdBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("vaultName") String vaultName,
								@JsonProperty("vaultDescription") String vaultDescription) implements AuditEventDto {
	}

	record VaultUpdatedEventDto(long id, Instant timestamp, String type, @JsonProperty("updatedBy") String updatedBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("vaultName") String vaultName,
								@JsonProperty("vaultDescription") String vaultDescription, @JsonProperty("vaultArchived") boolean vaultArchived) implements AuditEventDto {
	}

	record VaultAccessGrantedEventDto(long id, Instant timestamp, String type, @JsonProperty("grantedBy") String grantedBy, @JsonProperty("vaultId") UUID vaultId,
									  @JsonProperty("authorityId") String authorityId) implements AuditEventDto {
	}

	record VaultKeyRetrievedEventDto(long id, Instant timestamp, String type, @JsonProperty("retrievedBy") String retrievedBy, @JsonProperty("vaultId") UUID vaultId,
									 @JsonProperty("result") VaultKeyRetrievedEvent.Result result, @JsonProperty("ipAddress") String ipAddress, @JsonProperty("deviceId") String deviceId) implements AuditEventDto {
	}

	record VaultMemberAddedEventDto(long id, Instant timestamp, String type, @JsonProperty("addedBy") String addedBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("authorityId") String authorityId,
									@JsonProperty("role") VaultAccess.Role role) implements AuditEventDto {
	}

	record VaultMemberRemovedEventDto(long id, Instant timestamp, String type, @JsonProperty("removedBy") String removedBy, @JsonProperty("vaultId") UUID vaultId,
									  @JsonProperty("authorityId") String authorityId) implements AuditEventDto {
	}

	record VaultMemberUpdatedEventDto(long id, Instant timestamp, String type, @JsonProperty("updatedBy") String updatedBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("authorityId") String authorityId,
									  @JsonProperty("role") VaultAccess.Role role) implements AuditEventDto {
	}

	record VaultOwnershipClaimedEventDto(long id, Instant timestamp, String type, @JsonProperty("claimedBy") String claimedBy, @JsonProperty("vaultId") UUID vaultId) implements AuditEventDto {
	}

	record EmergencyAccessSetupEventDto(long id, Instant timestamp, String type, @JsonProperty("ownerId") String ownerId, @JsonProperty("settings") String settings,
										@JsonProperty("ipAddress") String ipAddress) implements AuditEventDto {
	}

	record EmergencyAccessSettingsUpdatedEventDto(long id, Instant timestamp, String type, @JsonProperty("adminId") String adminId, @JsonProperty("councilMemberIds") String councilMemberIds,
												  @JsonProperty("requiredKeyShares") int requiredKeyShares, @JsonProperty("allowChoosingCouncil") boolean allowChoosingCouncil) implements AuditEventDto {
	}

	record EmergencyAccessRecoveryStartedEventDto(long id, Instant timestamp, String type, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("processId") UUID processId,
												  @JsonProperty("councilMemberId") String councilMemberId, @JsonProperty("recoveryType") String recoveryType,
												  @JsonProperty("details") String details) implements AuditEventDto {
	}

	record EmergencyAccessRecoveryApprovedEventDto(long id, Instant timestamp, String type, @JsonProperty("processId") UUID processId, @JsonProperty("councilMemberId") String councilMemberId,
												   @JsonProperty("ipAddress") String ipAddress) implements AuditEventDto {
	}

	record EmergencyAccessRecoveryCompletedEventDto(long id, Instant timestamp, String type, @JsonProperty("processId") UUID processId, @JsonProperty("councilMemberId") String councilMemberId) implements AuditEventDto {
	}

}
