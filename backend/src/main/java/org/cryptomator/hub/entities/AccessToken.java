package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "access_token")
@NamedQuery(name = "AccessToken.get", query = """
			SELECT a
			FROM User u
			INNER JOIN EffectiveVaultAccess perm ON u.id = perm.id.authorityId
			INNER JOIN u.accessTokens a ON a.id.vaultId = :vaultId AND a.id.userId = u.id
			WHERE perm.id.vaultId = :vaultId AND u.id = :userId
		""")
public class AccessToken extends PanacheEntityBase {

	@EmbeddedId
	public AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	public User user;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	public Vault vault;

	@Column(name = "vault_key_jwe", nullable = false)
	public String jwe;

	public static AccessToken unlock(UUID vaultId, String userId) {
		try {
			return find("#AccessToken.get", Parameters.with("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AccessToken other = (AccessToken) o;
		return Objects.equals(id, other.id)
				&& Objects.equals(user, other.user)
				&& Objects.equals(vault, other.vault)
				&& Objects.equals(jwe, other.jwe);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, user, vault, jwe);
	}

	@Override
	public String toString() {
		return "Access{" +
				"id=" + id +
				", user=" + user.id +
				", vault=" + vault.id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		public String userId;
		public UUID vaultId;

		public AccessId(String userId, UUID vaultId) {
			this.userId = userId;
			this.vaultId = vaultId;
		}

		public AccessId() {
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AccessId other = (AccessId) o;
			return Objects.equals(userId, other.userId) //
					&& Objects.equals(vaultId, other.vaultId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, vaultId);
		}

		@Override
		public String toString() {
			return "AccessId{" +
					"userId='" + userId + '\'' +
					", vaultId='" + vaultId + '\'' +
					'}';
		}
	}
}
