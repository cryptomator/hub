package org.cryptomator.hub.persistence.entities;

import io.smallrye.context.api.NamedInstance;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "access")
@NamedQuery(name = "Access.get", query = "SELECT a FROM Access a WHERE a.device.id = :deviceId AND a.device.user.id = :userId AND a.id.vaultId = :vaultId")
@NamedQuery(name = "Access.revoke", query = "DELETE Access a WHERE a.id.deviceId = :deviceId AND a.id.vaultId = :vaultId")
public class Access {

	// FIXME @ManyToOne(...cascade = {CascadeType.REMOVE}) doesn't add 'ON DELETE CASCADE' to foreign keys

	@EmbeddedId
	private AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("deviceId")
	@JoinColumn(name = "device_id")
	private Device device;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	private Vault vault;

	@Column(name = "vault_specific_masterkey", nullable = false)
	private String deviceSpecificMasterkey;

	@Column(name = "ephemeral_public_key", nullable = false)
	private String ephemeralPublicKey;

	public AccessId getId() {
		return id;
	}

	public void setId(AccessId id) {
		this.id = id;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
	}

	public String getDeviceSpecificMasterkey() {
		return deviceSpecificMasterkey;
	}

	public void setDeviceSpecificMasterkey(String deviceSpecificMasterkey) {
		this.deviceSpecificMasterkey = deviceSpecificMasterkey;
	}

	public String getEphemeralPublicKey() {
		return ephemeralPublicKey;
	}

	public void setEphemeralPublicKey(String ephemeralPublicKey) {
		this.ephemeralPublicKey = ephemeralPublicKey;
	}

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
				", device=" + device.getId() +
				", vault=" + vault.getId() +
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
}
