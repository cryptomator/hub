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
@NamedQuery(name = "AccessToken.deleteByUser", query = "DELETE FROM AccessToken a WHERE a.id.userId = :userId")
@NamedQuery(name = "AccessToken.get", query = """
			SELECT token
			FROM AccessToken token
			INNER JOIN EffectiveVaultAccess perm ON token.id.vaultId = perm.id.vaultId AND token.id.userId = perm.id.authorityId
			WHERE token.id.vaultId = :vaultId AND token.id.userId = :userId
		""")
@Table(name = "access_token")
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

	@Column(name = "vault_masterkey", nullable = false)
	public String vaultKey;

	public static AccessToken unlock(UUID vaultId, String userId) {
		try {
			return find("#AccessToken.get", Parameters.with("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}


	public static void deleteByUser(String userId) {
		delete("#AccessToken.deleteByUser", Parameters.with("userId", userId));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AccessToken other = (AccessToken) o;
		return Objects.equals(id, other.id)
				&& Objects.equals(user, other.user)
				&& Objects.equals(vault, other.vault)
				&& Objects.equals(vaultKey, other.vaultKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, user, vault, vaultKey);
	}

	@Override
	public String toString() {
		return "Access{" +
				"id=" + id +
				", user=" + user.id +
				", vault=" + vault.id +
				", vaultKey='" + vaultKey + '\'' +
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
