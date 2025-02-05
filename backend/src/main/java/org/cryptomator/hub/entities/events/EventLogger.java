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

	public void logUserAccountSetupComplete(String completedBy, String userName) {
		var event = new UserAccountSetupCompleteEvent();
		event.setTimestamp(Instant.now());
		event.setCompletedBy(completedBy);
		event.setUserName(userName);
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

	public void logVaultKeyRetrieved(String retrievedBy, UUID vaultId, VaultKeyRetrievedEvent.Result result) {
		var event = new VaultKeyRetrievedEvent();
		event.setTimestamp(Instant.now());
		event.setRetrievedBy(retrievedBy);
		event.setVaultId(vaultId);
		event.setResult(result);
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

	//legacy
	public void logVaultOwnershipClaimed(String claimedBy, UUID vaultId) {
		var event = new VaultOwnershipClaimedEvent();
		event.setTimestamp(Instant.now());
		event.setClaimedBy(claimedBy);
		event.setVaultId(vaultId);
		auditEventRepository.persist(event);
	}
}
