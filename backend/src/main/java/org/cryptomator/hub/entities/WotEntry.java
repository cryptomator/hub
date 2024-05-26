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

@Entity
@Table(name = "wot")
public class WotEntry {

	@EmbeddedId
	private Id id;

	@Column(name = "signature", nullable = false)
	private String signature;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "user_id")
		private String userId;

		@Column(name = "signer_id")
		private String signerId;

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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Id other) {
				return Objects.equals(userId, other.userId) //
						&& Objects.equals(signerId, other.signerId);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, signerId);
		}

		@Override
		public String toString() {
			return "WotEntryId{" +
					"userId='" + userId + '\'' +
					", signerId='" + signerId + '\'' +
					'}';
		}
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<WotEntry, Id> {
	}
}
