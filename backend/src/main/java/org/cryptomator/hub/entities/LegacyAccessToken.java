package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "access_token_legacy")
@NamedQuery(name = "LegacyAccessToken.get", query = """
		SELECT token
		FROM LegacyAccessToken token
		INNER JOIN LegacyDevice device ON device.id = token.id.deviceId
		INNER JOIN EffectiveVaultAccess perm ON token.id.vaultId = perm.id.vaultId AND device.ownerId = perm.id.authorityId
		WHERE token.id.vaultId = :vaultId AND token.id.deviceId = :deviceId AND device.ownerId = :userId
		""")
@NamedQuery(name = "LegacyAccessToken.getByDevice", query = """
		SELECT token
		FROM LegacyAccessToken token
		INNER JOIN LegacyDevice device ON device.id = token.id.deviceId
		INNER JOIN EffectiveVaultAccess perm ON token.id.vaultId = perm.id.vaultId AND device.ownerId = perm.id.authorityId
		WHERE token.id.deviceId = :deviceId AND device.ownerId = :userId
		""")
@Deprecated
public class LegacyAccessToken {

	@EmbeddedId
	private AccessId id = new AccessId();

	@Column(name = "jwe", nullable = false)
	private String jwe;

	public AccessId getId() {
		return id;
	}

	public void setId(AccessId id) {
		this.id = id;
	}

	public String getJwe() {
		return jwe;
	}

	public void setJwe(String jwe) {
		this.jwe = jwe;
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
		private String deviceId;

		@Column(name = "vault_id", nullable = false)
		private UUID vaultId;

		public String getDeviceId() {
			return deviceId;
		}

		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}

		public UUID getVaultId() {
			return vaultId;
		}

		public void setVaultId(UUID vaultId) {
			this.vaultId = vaultId;
		}

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

	@ApplicationScoped
	@Deprecated
	public static class Repository implements PanacheRepositoryBase<LegacyAccessToken, AccessId> {

		public LegacyAccessToken unlock(UUID vaultId, String deviceId, String userId) {
			return find("#LegacyAccessToken.get", Map.of("deviceId", deviceId, "vaultId", vaultId, "userId", userId))
					.singleResult();
		}

		public Stream<LegacyAccessToken> getByDeviceAndOwner(String deviceId, String userId) {
			return stream("#LegacyAccessToken.getByDevice", Map.of("deviceId", deviceId, "userId", userId));
		}
	}
}