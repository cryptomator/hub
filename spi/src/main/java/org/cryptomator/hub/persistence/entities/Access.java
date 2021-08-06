package org.cryptomator.hub.persistence.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "access")
public class Access {

	// FIXME @ManyToOne(...cascade = {CascadeType.REMOVE}) doesn't add 'ON DELETE CASCADE' to foreign keys

	@EmbeddedId()
	private AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@JoinColumn(name = "device_id", insertable = false, updatable = false, nullable = false)
	private Device device;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@JoinColumn(name = "vault_id", insertable = false, updatable = false, nullable = false)
	private Vault vault;

	@Column(name = "deviceSpecificMasterkey", nullable = false)
	private String deviceSpecificMasterkey;

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

		private String device_id;
		private String vault_id;

		public AccessId(String device_id, String vault_id) {
			this.device_id = device_id;
			this.vault_id = vault_id;
		}

		public AccessId() {
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AccessId accessId = (AccessId) o;
			return Objects.equals(device_id, accessId.device_id) && Objects.equals(vault_id, accessId.vault_id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(device_id, vault_id);
		}
	}
}
