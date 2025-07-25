package org.cryptomator.hub.entities.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.VaultAccess;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class EventLogger {

	@Inject
	AuditEvent.Repository auditEventRepository;

	public void logVaultCreated(String createdBy, UUID vaultId, String vaultName, String vaultDescription) {
		var event = new VaultCreatedEvent();
		event.setTimestamp(Instant.now());
		event.setCreatedBy(createdBy);
		event.setVaultId(vaultId);
		event.setVaultName(vaultName);
		event.setVaultDescription(vaultDescription);
		auditEventRepository.persist(event);
	}

	public void logVaultUpdated(String updatedBy, UUID vaultId, String vaultName, String vaultDescription, boolean vaultArchived) {
		var event = new VaultUpdatedEvent();
		event.setTimestamp(Instant.now());
		event.setUpdatedBy(updatedBy);
		event.setVaultId(vaultId);
		event.setVaultName(vaultName);
		event.setVaultDescription(vaultDescription);
		event.setVaultArchived(vaultArchived);
		auditEventRepository.persist(event);
	}

	public void logDeviceRegisted(String registeredBy, String deviceId, String deviceName, Device.Type deviceType) {
		var event = new DeviceRegisteredEvent();
		event.setTimestamp(Instant.now());
		event.setRegisteredBy(registeredBy);
		event.setDeviceId(deviceId);
		event.setDeviceName(deviceName);
		event.setDeviceType(deviceType);
		auditEventRepository.persist(event);
	}

	public void logDeviceRemoved(String removedBy, String deviceId) {
		var event = new DeviceRemovedEvent();
		event.setTimestamp(Instant.now());
		event.setRemovedBy(removedBy);
		event.setDeviceId(deviceId);
		auditEventRepository.persist(event);
	}

	public void logUserAccountReset(String resetBy) {
		var event = new UserAccountResetEvent();
		event.setTimestamp(Instant.now());
		event.setResetBy(resetBy);
		auditEventRepository.persist(event);
	}

	public void logUserKeysChanged(String changedBy, String userName) {
		var event = new UserKeysChangeEvent();
		event.setTimestamp(Instant.now());
		event.setChangedBy(changedBy);
		event.setUserName(userName);
		auditEventRepository.persist(event);
	}

	public void logUserSetupCodeChanged(String changedBy) {
		var event = new UserSetupCodeChangeEvent();
		event.setTimestamp(Instant.now());
		event.setChangedBy(changedBy);
		auditEventRepository.persist(event);
	}

	public void logVaultAccessGranted(String grantedBy, UUID vaultId, String authorityId) {
		var event = new VaultAccessGrantedEvent();
		event.setTimestamp(Instant.now());
		event.setGrantedBy(grantedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		auditEventRepository.persist(event);
	}

	public void logVaultKeyRetrieved(String retrievedBy, UUID vaultId, VaultKeyRetrievedEvent.Result result, String ipAddress, String deviceId) {
		var event = new VaultKeyRetrievedEvent();
		event.setTimestamp(Instant.now());
		event.setRetrievedBy(retrievedBy);
		event.setVaultId(vaultId);
		event.setResult(result);
		event.setIpAddress(ipAddress);
		event.setDeviceId(deviceId);
		auditEventRepository.persist(event);
	}

	public void logVaultMemberAdded(String addedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
		var event = new VaultMemberAddedEvent();
		event.setTimestamp(Instant.now());
		event.setAddedBy(addedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		event.setRole(role);
		auditEventRepository.persist(event);
	}

	public void logVaultMemberRemoved(String removedBy, UUID vaultId, String authorityId) {
		var event = new VaultMemberRemovedEvent();
		event.setTimestamp(Instant.now());
		event.setRemovedBy(removedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		auditEventRepository.persist(event);
	}

	public void logVaultMemberUpdated(String updatedBy, UUID vaultId, String authorityId, VaultAccess.Role role) {
		var event = new VaultMemberUpdatedEvent();
		event.setTimestamp(Instant.now());
		event.setUpdatedBy(updatedBy);
		event.setVaultId(vaultId);
		event.setAuthorityId(authorityId);
		event.setRole(role);
		auditEventRepository.persist(event);
	}

	public void logWotSettingUpdated(String updatedBy, int wotIdVerifyLen, int wotMaxDepth) {
		var event = new SettingWotUpdateEvent();
		event.setTimestamp(Instant.now());
		event.setWotIdVerifyLen(wotIdVerifyLen);
		event.setWotMaxDepth(wotMaxDepth);
		event.setUpdatedBy(updatedBy);
		auditEventRepository.persist(event);
	}

	public void logWotIdSigned(String userId, String signerId, String signerKey, String signature) {
		var event = new SignedWotIdEvent();
		event.setTimestamp(Instant.now());
		event.setUserId(userId);
		event.setSignerId(signerId);
		event.setSignerKey(signerKey);
		event.setSignature(signature);
		auditEventRepository.persist(event);
	}

	//region Emergency Access

	public void logEmergencyAccessSetup(UUID vaultId, String ownerId, String settings, String ipAddress) {
		var event = new EmergencyAccessSetupEvent();
		event.setTimestamp(Instant.now());
		event.setVaultId(vaultId);
		event.setOwnerId(ownerId);
		event.setSettings(settings);
		event.setIpAddress(ipAddress);
		auditEventRepository.persist(event);
	}

	public void logEmergencyAccessSettingsUpdated(String adminId, String councilMemberIds, int requiredKeyShares, boolean allowChoosingCouncil) {
		var event = new EmergencyAccessSettingsUpdatedEvent();
		event.setTimestamp(Instant.now());
		event.setAdminId(adminId);
		event.setCouncilMemberIds(councilMemberIds);
		event.setRequiredKeyShares(requiredKeyShares);
		event.setAllowChoosingCouncil(allowChoosingCouncil);
		auditEventRepository.persist(event);
	}

	public void logEmergencyAccessRecoveryStarted(UUID vaultId, UUID processId, String councilMemberId, String type, String details) {
		var event = new EmergencyAccessRecoveryStartedEvent();
		event.setTimestamp(Instant.now());
		event.setVaultId(vaultId);
		event.setProcessId(processId);
		event.setCouncilMemberId(councilMemberId);
		event.setProcessType(type);
		event.setDetails(details);
		auditEventRepository.persist(event);
	}

	public void logEmergencyAccessRecoveryApproved(UUID processId, String councilMemberId, String ipAddress) {
		var event = new EmergencyAccessRecoveryApprovedEvent();
		event.setTimestamp(Instant.now());
		event.setProcessId(processId);
		event.setCouncilMemberId(councilMemberId);
		event.setIpAddress(ipAddress);
		auditEventRepository.persist(event);
	}

	public void logEmergencyAccessRecoveryCompleted(UUID processId, String councilMemberId) {
		var event = new EmergencyAccessRecoveryCompletedEvent();
		event.setTimestamp(Instant.now());
		event.setProcessId(processId);
		event.setCouncilMemberId(councilMemberId);
		auditEventRepository.persist(event);
	}

	//endregion

	//legacy
	public void logVaultOwnershipClaimed(String claimedBy, UUID vaultId) {
		var event = new VaultOwnershipClaimedEvent();
		event.setTimestamp(Instant.now());
		event.setClaimedBy(claimedBy);
		event.setVaultId(vaultId);
		auditEventRepository.persist(event);
	}
}
