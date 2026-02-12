package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_emergaccess_recovery_aborted")
@DiscriminatorValue(EmergencyAccessRecoveryAbortedEvent.TYPE)
public class EmergencyAccessRecoveryAbortedEvent extends AuditEvent {

    public static final String TYPE = "EMERGENCY_ACCESS_RECOVERY_ABORTED";

    @Column(name = "vault_id", nullable = false)
    private UUID vaultId;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "council_member_id", nullable = false)
    private String councilMemberId;

    @Column(name = "ip_address")
    private String ipAddress;

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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmergencyAccessRecoveryAbortedEvent other = (EmergencyAccessRecoveryAbortedEvent) o;
        return Objects.equals(vaultId, other.vaultId)
                && Objects.equals(processId, other.processId)
                && Objects.equals(councilMemberId, other.councilMemberId)
                && Objects.equals(ipAddress, other.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vaultId, processId, councilMemberId, ipAddress);
    }
}
