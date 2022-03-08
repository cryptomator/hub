package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "device")
@NamedQuery(name = "Device.requiringUserAccessGrant",
		query = """
				SELECT d
				FROM Vault v
					INNER JOIN v.members m
					INNER JOIN m.devices d
					LEFT JOIN d.userAccess a ON a.id.vaultId = :vaultId AND a.id.deviceId = d.id
					WHERE v.id = :vaultId AND a.vault IS NULL
				"""
)
@NamedQuery(name = "Device.requiringGroupAccessGrant",
		query = """
				SELECT d
				FROM Vault v
					INNER JOIN v.groups g
					INNER JOIN g.members m
					INNER JOIN m.devices d
					LEFT JOIN d.groupAccess a ON a.id.vaultId = :vaultId AND a.id.deviceId = d.id
					WHERE v.id = :vaultId AND a.vault IS NULL
				"""
)
public class Device extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", updatable = false, nullable = false)
	public User owner;

	@OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
	public Set<UserAccess> userAccess = new HashSet<>();

	@OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
	public Set<GroupAccess> groupAccess = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "publickey", nullable = false)
	public String publickey;

	@Override
	public String toString() {
		return "Device{" +
				"id='" + id + '\'' +
				", owner=" + owner.id +
				", name='" + name + '\'' +
				", publickey='" + publickey + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Device other = (Device) o;
		return Objects.equals(this.id, other.id)
				&& Objects.equals(this.owner, other.owner)
				&& Objects.equals(this.name, other.name)
				&& Objects.equals(this.publickey, other.publickey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, owner, name, publickey);
	}

	public static Stream<Device> findRequiringAccessGrant(String vaultId) {
		return Stream.<Device>concat(find("#Device.requiringUserAccessGrant", Parameters.with("vaultId", vaultId)).stream(),
				find("#Device.requiringGroupAccessGrant", Parameters.with("vaultId", vaultId)).stream()).collect(Collectors.toSet()).stream();
	}

}
