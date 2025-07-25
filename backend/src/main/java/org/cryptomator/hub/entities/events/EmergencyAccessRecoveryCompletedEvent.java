package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_emergaccess_recovery_completed")
@DiscriminatorValue(EmergencyAccessRecoveryCompletedEvent.TYPE)
public class EmergencyAccessRecoveryCompletedEvent extends AuditEvent {

	public static final String TYPE = "EMERGENCY_ACCESS_RECOVERY_COMPLETED";

	@Column(name = "process_id", nullable = false)
	private UUID processId;

	@Column(name = "council_member_id", nullable = false)
	private String councilMemberId;

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

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		EmergencyAccessRecoveryCompletedEvent other = (EmergencyAccessRecoveryCompletedEvent) o;
		return Objects.equals(processId, other.processId)
				&& Objects.equals(councilMemberId, other.councilMemberId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), processId, councilMemberId);
	}
}

