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
@Table(name = "audit_event_sign_wot_id")
@DiscriminatorValue(SignedWotIdEvent.TYPE)
public class SignedWotIdEvent extends AuditEvent {

	public static final String TYPE = "SIGN_WOT_ID";

	@Column(name = "user_id")
	private String userId;

	@Column(name = "signer_id")
	private String signerId;

	@Column(name = "signer_key")
	private String signerKey;

	@Column(name = "signature")
	private String signature;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSignerId() {
		return signerId;
	}

	public void setSignerId(String signerId) {
		this.signerId = signerId;
	}

	public String getSignerKey() {
		return signerKey;
	}

	public void setSignerKey(String signerKey) {
		this.signerKey = signerKey;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SignedWotIdEvent that = (SignedWotIdEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(signerId, that.signerId) //
				&& Objects.equals(signerKey, that.signerKey) //
				&& Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), userId, signerId, signerKey, signature);
	}

}
