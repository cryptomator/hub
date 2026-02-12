package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovered_emergency_key_shares")
public class RecoveredEmergencyKeyShares {

	@EmbeddedId
	private Id id = new RecoveredEmergencyKeyShares.Id();

	@Column(name = "process_private_key", nullable = false)
	private String processPrivateKey;

	@Column(name = "unrecovered_key_share", nullable = false)
	private String unrecoveredKeyShare;

	@Column(name = "recovered_key_share")
	private String recoveredKeyShare;

	@Column(name = "signed_process_info")
	private String signedProcessInfo;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getProcessPrivateKey() {
		return processPrivateKey;
	}

	public void setProcessPrivateKey(String processPrivateKey) {
		this.processPrivateKey = processPrivateKey;
	}

	public String getUnrecoveredKeyShare() {
		return unrecoveredKeyShare;
	}

	public void setUnrecoveredKeyShare(String unrecoveredKeyShare) {
		this.unrecoveredKeyShare = unrecoveredKeyShare;
	}

	public String getRecoveredKeyShare() {
		return recoveredKeyShare;
	}

	public void setRecoveredKeyShare(String recoveredKeyShare) {
		this.recoveredKeyShare = recoveredKeyShare;
	}

	public String getSignedProcessInfo() {
		return signedProcessInfo;
	}

	public void setSignedProcessInfo(String signedProcessInfo) {
		this.signedProcessInfo = signedProcessInfo;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		RecoveredEmergencyKeyShares other = (RecoveredEmergencyKeyShares) o;
		return Objects.equals(id, other.id)
				&& Objects.equals(processPrivateKey, other.processPrivateKey)
				&& Objects.equals(unrecoveredKeyShare, other.unrecoveredKeyShare)
				&& Objects.equals(recoveredKeyShare, other.recoveredKeyShare)
				&& Objects.equals(signedProcessInfo, other.signedProcessInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, processPrivateKey, unrecoveredKeyShare, recoveredKeyShare, signedProcessInfo);
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "council_member_id")
		private String councilMemberId;

		@Column(name = "recovery_process_id")
		private UUID recoveryId;

		public String getCouncilMemberId() {
			return councilMemberId;
		}

		public void setCouncilMemberId(String councilMemberId) {
			this.councilMemberId = councilMemberId;
		}

		public UUID getRecoveryId() {
			return recoveryId;
		}

		public void setRecoveryId(UUID recoveryId) {
			this.recoveryId = recoveryId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Id other) {
				return Objects.equals(councilMemberId, other.councilMemberId) //
						&& Objects.equals(recoveryId, other.recoveryId);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(councilMemberId, recoveryId);
		}

		@Override
		public String toString() {
			return "RecoveredEmergencyKeySharesId{" +
					"councilMemberId='" + councilMemberId + '\'' +
					", recoveryId='" + recoveryId + '\'' +
					'}';
		}
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<RecoveredEmergencyKeyShares, Id> {
	}
}
