package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "emergency_recovery_processes")
@NamedQuery(name = "EmergencyRecoveryProcess.findByVaultId", query = """
			SELECT process
			FROM EmergencyRecoveryProcess process
			WHERE process.vaultId = :vaultId
		""")
@NamedQuery(
		name = "EmergencyRecoveryProcess.byCouncilMember", query = """
		    SELECT process
		    FROM EmergencyRecoveryProcess process
		    WHERE KEY(process.recoveredKeyShares) = :councilMemberId
		"""
)
public class EmergencyRecoveryProcess {

	public enum Type {
		ASSIGN_OWNER,
		COUNCIL_CHANGE
	}

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "vault_id", nullable = false)
	private UUID vaultId;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private Type type;

	@Column(name = "details")
	private String details;

	@Column(name = "required_key_shares", nullable = false)
	private int requiredKeyShares;

	@Column(name = "process_public_key", nullable = false)
	private String processPublicKey;

	@OneToMany(mappedBy = "id.recoveryId", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@MapKey(name = "id.councilMemberId")
	private Map<String, RecoveredEmergencyKeyShares> recoveredKeyShares = new HashMap<>();

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public int getRequiredKeyShares() {
		return requiredKeyShares;
	}

	public void setRequiredKeyShares(int requiredKeyShares) {
		this.requiredKeyShares = requiredKeyShares;
	}

	public String getProcessPublicKey() {
		return processPublicKey;
	}

	public void setProcessPublicKey(String processPublicKey) {
		this.processPublicKey = processPublicKey;
	}

	public Map<String, RecoveredEmergencyKeyShares> getRecoveredKeyShares() {
		return Map.copyOf(recoveredKeyShares);
	}

	public void setRecoveredKeyShares(Map<String, RecoveredEmergencyKeyShares> recoveredKeyShares) {
		this.recoveredKeyShares.clear();
		this.recoveredKeyShares.putAll(recoveredKeyShares);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		EmergencyRecoveryProcess that = (EmergencyRecoveryProcess) o;
		return requiredKeyShares == that.requiredKeyShares && Objects.equals(id, that.id) && Objects.equals(vaultId, that.vaultId) && Objects.equals(type, that.type) && Objects.equals(details, that.details) && Objects.equals(processPublicKey, that.processPublicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, vaultId, type, details, requiredKeyShares, processPublicKey);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EmergencyRecoveryProcess, UUID> {

		public Stream<EmergencyRecoveryProcess> findByVaultId(UUID vaultId) {
			try {
				return find("#EmergencyRecoveryProcess.findByVaultId", Parameters.with("vaultId", vaultId)).stream();
			} catch (NoResultException e) {
				return null;
			}
		}

		public Stream<EmergencyRecoveryProcess> findByCouncilMember(String councilMemberId) {
			return find("#EmergencyRecoveryProcess.byCouncilMember", Parameters.with("councilMemberId", councilMemberId)).stream();
		}

		// Deletes unrecovered key shares, keeps recovered ones. After resetting account, council member will no longer be able to recover their key share
		@Transactional
		public void deleteUnrecoveredKeySharesForCouncilMember(String councilMemberId) {
			var adjustedProcesses = findByCouncilMember(councilMemberId).map(process -> {
				process.recoveredKeyShares.computeIfPresent(councilMemberId, (unused, share) -> {
					if (share.getRecoveredKeyShare() != null) {
						return share; // keep entry, if already recovered
					} else {
						return null; // remove otherwise
					}
				});
				return process;
			});
			persist(adjustedProcesses);
		}
	}

}
