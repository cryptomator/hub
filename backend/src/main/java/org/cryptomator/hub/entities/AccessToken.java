package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
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
		/*
		 * FIXME remove this native query and add the named query again as soon as Hibernate ORM ships version 6.2.8 or 6.3.0
		 * See https://github.com/quarkusio/quarkus/issues/35386 for further information
		 */

		try {
			var query = getEntityManager()
					.createNativeQuery("""
							select
							    a1_0."user_id",
							    a1_0."vault_id",
							    u1_0."id",
							    u1_1."name",
							    u1_0."email",
							    u1_0."picture_url",
							    u1_0."privatekey",
							    u1_0."publickey",
							    u1_0."setupcode",
							    a1_0."vault_masterkey"
							from
							    "user_details" u1_0
							join
								"authority" u1_1
									on u1_0."id"=u1_1."id"
							join
							    "effective_vault_access" e1_0
							        on u1_0."id"=e1_0."authority_id"
							join
							    "access_token" a1_0
							        on u1_0."id"=a1_0."user_id"
							        and a1_0."vault_id"=:vaultId
							        and a1_0."user_id"=u1_0."id"
							where
							    e1_0."vault_id"=:vaultId
							    and u1_0."id"=:userId
									""", AccessToken.class)
					.setParameter("vaultId", vaultId)
					.setParameter("userId", userId);
			return (AccessToken) query.getSingleResult();
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
