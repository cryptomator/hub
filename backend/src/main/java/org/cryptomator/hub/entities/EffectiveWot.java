package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import java.util.Map;

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
	public record Id(
			@Column(name = "trusting_user_id") String trustingUserId,
			@Column(name = "trusted_user_id") String trustedUserId) {
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EffectiveWot, Id> {
		public PanacheQuery<EffectiveWot> findTrusted(String trustingUserId) {
			return find("#EffectiveWot.findTrustedUsers", Map.of("trustingUserId", trustingUserId));
		}

		public PanacheQuery<EffectiveWot> findTrusted(String trustingUserId, String trustedUserId) {
			return find("#EffectiveWot.findTrustedUser", Map.of("trustingUserId", trustingUserId, "trustedUserId", trustedUserId));
		}
	}
}
