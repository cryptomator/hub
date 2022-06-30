package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "access_token")
@NamedQuery(name = "AccessToken.get", query = """
			SELECT a
			FROM Vault v
			INNER JOIN v.effectiveMembers u
			INNER JOIN u.devices d
			INNER JOIN d.accessTokens a ON a.id.deviceId = d.id AND a.id.vaultId = v.id
			WHERE v.id = :vaultId
				AND u.id = :userId
				AND d.id = :deviceId
		""")
public class AccessToken extends PanacheEntityBase {

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

	public static AccessToken unlock(String vaultId, String deviceId, String userId) {
		try {
			return find("#AccessToken.get", Parameters.with("deviceId", deviceId).and("vaultId", vaultId).and("userId", userId)).firstResult();
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
		return "Access{" +
				"id=" + id +
				", device=" + device.id +
				", vault=" + vault.id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		public String deviceId;
		public String vaultId;

		public AccessId(String deviceId, String vaultId) {
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
			return "AccessId{" +
					"deviceId='" + deviceId + '\'' +
					", vaultId='" + vaultId + '\'' +
					'}';
		}
	}
}
