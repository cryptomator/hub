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
@Table(name = "group_access")
@NamedQuery(name = "GroupAccess.get", query = """
			SELECT a
			FROM GroupAccess a
			INNER JOIN a.group.members m
			WHERE a.device.id = :deviceId
				AND a.id.vaultId = :vaultId
				AND m.id = :userId
		""")
public class GroupAccess extends PanacheEntityBase {

	@EmbeddedId
	public AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("deviceId")
	@JoinColumn(name = "device_id")
	public Device device;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("groupId")
	@JoinColumn(name = "group_id")
	public Group group;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	public Vault vault;

	@Column(name = "jwe", nullable = false)
	public String jwe;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GroupAccess access = (GroupAccess) o;
		return Objects.equals(id, access.id)
				&& Objects.equals(device, access.device)
				&& Objects.equals(group, access.group)
				&& Objects.equals(vault, access.vault)
				&& Objects.equals(jwe, access.jwe);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, device, group, vault, jwe);
	}

	@Override
	public String toString() {
		return "Access{" +
				"id=" + id +
				", device=" + device.id +
				", user=" + group.id +
				", vault=" + vault.id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		@Column(name = "device_id", nullable = false)
		private String deviceId;

		@Column(name = "group_id", nullable = false)
		private String groupId;

		@Column(name = "vault_id", nullable = false)
		private String vaultId;

		public AccessId(String deviceId, String groupId, String vaultId) {
			this.deviceId = deviceId;
			this.groupId = groupId;
			this.vaultId = vaultId;
		}

		public AccessId() {
		}

		public String getDeviceId() {
			return deviceId;
		}

		public String getVaultId() {
			return vaultId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AccessId other = (AccessId) o;
			return Objects.equals(deviceId, other.deviceId) //
					&& Objects.equals(groupId, other.groupId) //
					&& Objects.equals(vaultId, other.vaultId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(deviceId, groupId, vaultId);
		}
	}

	// --- data layer queries ---

	public static GroupAccess unlock(String vaultId, String deviceId, String userId) {
		try {
			return find("#GroupAccess.get", Parameters.with("deviceId", deviceId).and("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}

}
