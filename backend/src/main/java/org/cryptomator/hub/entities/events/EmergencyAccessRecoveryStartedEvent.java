package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_emergaccess_recovery_started")
@DiscriminatorValue(EmergencyAccessRecoveryStartedEvent.TYPE)
public class EmergencyAccessRecoveryStartedEvent extends AuditEvent {

	public static final String TYPE = "EMERGENCY_ACCESS_RECOVERY_STARTED";

	@Column(name = "vault_id", nullable = false)
	private UUID vaultId;

	@Column(name = "process_id", nullable = false)
	private UUID processId;

	@Column(name = "council_member_id", nullable = false)
	private String councilMemberId;

	@Column(name = "process_type", nullable = false)
	private String processType;

	@Column(name = "details", nullable = false)
	private String details;

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public UUID getProcessId() {
		return processId;
	}

	public void setProcessId(UUID processId) {
		this.processId = processId;
	}

	public String getCouncilMemberId() {
		return councilMemberId;
	}

	public void setCouncilMemberId(String councilMemberId) {
		this.councilMemberId = councilMemberId;
	}

	public String getProcessType() {
		return processType;
	}

	public void setProcessType(String processType) {
		this.processType = processType;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		EmergencyAccessRecoveryStartedEvent other = (EmergencyAccessRecoveryStartedEvent) o;
		return Objects.equals(vaultId, other.vaultId)
				&& Objects.equals(processId, other.processId)
				&& Objects.equals(councilMemberId, other.councilMemberId)
				&& Objects.equals(processType, other.processType)
				&& Objects.equals(details, other.details);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), vaultId, processId, councilMemberId, processType, details);
	}
}
