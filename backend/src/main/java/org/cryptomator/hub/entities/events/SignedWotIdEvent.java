package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "audit_event_sign_wot_id")
@DiscriminatorValue(SignedWotIdEvent.TYPE)
public class SignedWotIdEvent extends AuditEvent {

	public static final String TYPE = "SIGN_WOT_ID";

	@Column(name = "user_id")
	private @Nullable String userId;

	@Column(name = "signer_id")
	private @Nullable String signerId;

	@Column(name = "signer_key")
	private @Nullable String signerKey;

	@Column(name = "signature")
	private @Nullable String signature;

	public @Nullable String getUserId() {
		return userId;
	}

	public void setUserId(@Nullable String userId) {
		this.userId = userId;
	}

	public @Nullable String getSignerId() {
		return signerId;
	}

	public void setSignerId(@Nullable String signerId) {
		this.signerId = signerId;
	}

	public @Nullable String getSignerKey() {
		return signerKey;
	}

	public void setSignerKey(@Nullable String signerKey) {
		this.signerKey = signerKey;
	}

	public @Nullable String getSignature() {
		return signature;
	}

	public void setSignature(@Nullable String signature) {
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
