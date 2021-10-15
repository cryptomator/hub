package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "access")
@NamedQuery(name = "Access.get", query = """
			SELECT a
			FROM Access a
			WHERE a.device.id = :deviceId
				AND a.device.owner.id = :userId
				AND a.id.vaultId = :vaultId
		""")
@NamedQuery(name = "Access.revokeDevice", query = "DELETE FROM Access a WHERE a.id.deviceId = :deviceId AND a.id.vaultId = :vaultId")
@NamedQuery(name = "Access.revokeUser", query = """
			DELETE
			FROM Access a
			WHERE a.id.vaultId = :vaultId
				AND a.id.deviceId IN (SELECT d.id FROM Device d WHERE d.owner.id = :userId)
		""")
public class Access extends PanacheEntityBase {

	// FIXME @ManyToOne(...cascade = {CascadeType.REMOVE}) doesn't add 'ON DELETE CASCADE' to foreign keys

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

	@Column(name = "device_specific_masterkey", nullable = false)
	public String deviceSpecificMasterkey;

	@Column(name = "ephemeral_public_key", nullable = false)
	public String ephemeralPublicKey;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Access access = (Access) o;
		return Objects.equals(id, access.id)
				&& Objects.equals(device, access.device)
				&& Objects.equals(vault, access.vault)
				&& Objects.equals(deviceSpecificMasterkey, access.deviceSpecificMasterkey);
	}

    /*@Override
    public int hashCode() {
        return Objects.hash(id, device, vault, deviceSpecificMasterkey);
    }*/

	@Override
	public String toString() {
		return "Access{" +
				"id=" + id +
				", device=" + device.id +
				", vault=" + vault.id +
				", deviceSpecificMasterkey='" + deviceSpecificMasterkey + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		@Column(name = "device_id", nullable = false)
		private String deviceId;

		@Column(name = "vault_id", nullable = false)
		private String vaultId;

		public AccessId(String deviceId, String vaultId) {
			this.deviceId = deviceId;
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
			AccessId accessId = (AccessId) o;
			return Objects.equals(deviceId, accessId.deviceId) && Objects.equals(vaultId, accessId.vaultId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(deviceId, vaultId);
		}
	}

	// --- data layer queries ---

	public static Access unlock(String vaultId, String deviceId, String userId) {
		try {
			return find("#Access.get", Parameters.with("deviceId", deviceId).and("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public static void deleteDeviceAccess(String vaultId, String deviceId) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeDevice").setParameter("vaultId", vaultId).setParameter("deviceId", deviceId).executeUpdate();
		if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + deviceId + ") not found");
		}
	}

	public static void deleteUserAccess(String vaultId, String userId) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeUser").setParameter("vaultId", vaultId).setParameter("userId", userId).executeUpdate();
		if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", user: " + userId + ") not found");
		}
	}

}
