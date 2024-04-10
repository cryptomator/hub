package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event_vault_key_retrieve")
@DiscriminatorValue(VaultKeyRetrievedEvent.TYPE)
public class VaultKeyRetrievedEvent extends AuditEvent {

	public static final String TYPE = "VAULT_KEY_RETRIEVE";

	@Column(name = "retrieved_by")
	private String retrievedBy;

	@Column(name = "vault_id")
	private UUID vaultId;

	@Column(name = "result")
	@Enumerated(EnumType.STRING)
	private Result result;

	public String getRetrievedBy() {
		return retrievedBy;
	}

	public void setRetrievedBy(String retrievedBy) {
		this.retrievedBy = retrievedBy;
	}

	public UUID getVaultId() {
		return vaultId;
	}

	public void setVaultId(UUID vaultId) {
		this.vaultId = vaultId;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultKeyRetrievedEvent that = (VaultKeyRetrievedEvent) o;
		return super.equals(that) //
				&& Objects.equals(retrievedBy, that.retrievedBy) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), retrievedBy, vaultId, result);
	}

	public enum Result {
		SUCCESS,
		UNAUTHORIZED
	}

}
