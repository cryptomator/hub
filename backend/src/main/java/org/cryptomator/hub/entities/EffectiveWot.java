package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Immutable
@Table(name = "effective_wot")
@NamedQuery(name = "EffectiveWot.findTrustedUsers", query = """
		SELECT wot
		FROM EffectiveWot wot
		WHERE wot.id.trustingUserId = :trustingUserId
		""")
@NamedQuery(name = "EffectiveWot.findTrustedUser", query = """
		SELECT wot
		FROM EffectiveWot wot
		WHERE wot.id.trustingUserId = :trustingUserId AND wot.id.trustedUserId = :trustedUserId
		""")
public class EffectiveWot {

	@EmbeddedId
	private Id id;

	@Column(name = "signature_chain")
	@Type(StringArrayType.class)
	private String[] signatureChain;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String[] getSignatureChain() {
		return signatureChain;
	}

	public void setSignatureChain(String[] signatureChain) {
		this.signatureChain = signatureChain;
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "trusting_user_id")
		private String trustingUserId;

		@Column(name = "trusted_user_id")
		private String trustedUserId;

		public String getTrustingUserId() {
			return trustingUserId;
		}

		public String getTrustedUserId() {
			return trustedUserId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Id other) {
				return Objects.equals(trustingUserId, other.trustingUserId) //
						&& Objects.equals(trustedUserId, other.trustedUserId);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(trustingUserId, trustedUserId);
		}

		@Override
		public String toString() {
			return "EffectiveWotId{" +
					"trustingUserId='" + trustingUserId + '\'' +
					", trustedUserId='" + trustedUserId + '\'' +
					'}';
		}
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EffectiveWot, Id> {
		public PanacheQuery<EffectiveWot> findTrusted(String trustingUserId) {
			return find("#EffectiveWot.findTrustedUsers", Parameters.with("trustingUserId", trustingUserId));
		}

		public PanacheQuery<EffectiveWot> findTrusted(String trustingUserId, String trustedUserId) {
			return find("#EffectiveWot.findTrustedUser", Parameters.with("trustingUserId", trustingUserId).and("trustedUserId", trustedUserId));
		}
	}
}
