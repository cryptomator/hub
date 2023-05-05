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
@Table(name = "access_token_legacy")
@NamedQuery(name = "LegacyAccessToken.get", query = """
			SELECT t
			FROM LegacyAccessToken t
			INNER JOIN t.device d
			INNER JOIN EffectiveVaultAccess a ON a.id.vaultId = t.id.vaultId AND a.id.authorityId = d.owner.id
			WHERE t.id.vaultId = :vaultId
				AND d.owner.id = :userId
				AND d.id = :deviceId
		""")
@Deprecated
public class LegacyAccessToken extends PanacheEntityBase {

	@EmbeddedId
	public AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("deviceId")
	@JoinColumn(name = "device_id")
	public Device device;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	public Vault vault;

	@Column(name = "jwe", nullable = false)
	public String jwe;

	public static LegacyAccessToken unlock(UUID vaultId, String deviceId, String userId) {
		try {
			return find("#LegacyAccessToken.get", Parameters.with("deviceId", deviceId).and("vaultId", vaultId).and("userId", userId)).singleResult();
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
				&& Objects.equals(device, other.device)
				&& Objects.equals(vault, other.vault)
				&& Objects.equals(jwe, other.jwe);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, device, vault, jwe);
	}

	@Override
	public String toString() {
		return "LegacyAccessToken{" +
				"id=" + id +
				", device=" + device.id +
				", vault=" + vault.id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		public String deviceId;
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