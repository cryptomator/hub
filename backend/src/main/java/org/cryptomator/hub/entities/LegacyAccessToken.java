package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "access_token_legacy")
@NamedQuery(name = "LegacyAccessToken.get", query = """
		SELECT token
		FROM LegacyAccessToken token
		INNER JOIN LegacyDevice device ON device.id = token.id.deviceId
		INNER JOIN EffectiveVaultAccess perm ON token.id.vaultId = perm.id.vaultId AND device.ownerId = perm.id.authorityId
		WHERE token.id.vaultId = :vaultId AND token.id.deviceId = :deviceId AND device.ownerId = :userId
		""")
@Deprecated
public class LegacyAccessToken extends PanacheEntityBase {

	@EmbeddedId
	public AccessId id = new AccessId();

	@Column(name = "jwe", nullable = false)
	public String jwe;

	public static LegacyAccessToken unlock(UUID vaultId, String deviceId, String userId) {
		try {
			return getEntityManager().createNamedQuery("LegacyAccessToken.get", LegacyAccessToken.class) //
					.setParameter("deviceId", deviceId) //
					.setParameter("vaultId", vaultId) //
					.setParameter("userId", userId) //
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LegacyAccessToken other = (LegacyAccessToken) o;
		return Objects.equals(id, other.id)
				&& Objects.equals(jwe, other.jwe);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, jwe);
	}

	@Override
	public String toString() {
		return "LegacyAccessToken{" +
				"id=" + id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		@Column(name = "device_id", nullable = false)
		public String deviceId;

		@Column(name = "vault_id", nullable = false)
		public UUID vaultId;

		public AccessId(String deviceId, UUID vaultId) {
			this.deviceId = deviceId;
			this.vaultId = vaultId;
		}

		public AccessId() {
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AccessId other = (AccessId) o;
			return Objects.equals(deviceId, other.deviceId) //
					&& Objects.equals(vaultId, other.vaultId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(deviceId, vaultId);
		}

		@Override
		public String toString() {
			return "LegacyAccessTokenId{" +
					"deviceId='" + deviceId + '\'' +
					", vaultId='" + vaultId + '\'' +
					'}';
		}
	}
}