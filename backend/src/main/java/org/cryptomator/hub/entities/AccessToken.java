package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
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
@NamedQuery(name = "AccessToken.deleteByUser", query = """
			DELETE
			FROM AccessToken a
			WHERE a.id.userId = :userId
		""")
@NamedQuery(name = "AccessToken.get", query = """
			SELECT token
			FROM AccessToken token
			INNER JOIN EffectiveVaultAccess perm ON token.id.vaultId = perm.id.vaultId AND token.id.userId = perm.id.authorityId
			WHERE token.id.vaultId = :vaultId AND token.id.userId = :userId
		""")
public class AccessToken {

	@EmbeddedId
	AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	User user;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	Vault vault;

	@Column(name = "vault_masterkey", nullable = false)
	String vaultKey;

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

	public AccessId getId() {
		return id;
	}

	public void setId(AccessId id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
	}

	public String getVaultKey() {
		return vaultKey;
	}

	public void setVaultKey(String vaultKey) {
		this.vaultKey = vaultKey;
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

		String userId;
		UUID vaultId;

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public UUID getVaultId() {
			return vaultId;
		}

		public void setVaultId(UUID vaultId) {
			this.vaultId = vaultId;
		}

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

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<AccessToken, AccessId> {

		public AccessToken unlock(UUID vaultId, String userId) {
			try {
				return find("#AccessToken.get", Parameters.with("vaultId", vaultId).and("userId", userId)).firstResult();
			} catch (NoResultException e) {
				return null;
			}
		}

		public void deleteByUser(String userId) {
			delete("#AccessToken.deleteByUser", Parameters.with("userId", userId));
		}
	}
}
