package org.cryptomator.hub.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "device")
public class Device extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", updatable = false, nullable = false)
	public User owner;

	@OneToMany(mappedBy = "device", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Access> access = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "publickey", nullable = false)
	public String publickey;

	public void setAccess(Set<Access> access) {
		this.access.clear();
		this.access.addAll(access);
	}

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
				&& Objects.equals(this.access, other.access)
				&& Objects.equals(this.name, other.name)
				&& Objects.equals(this.publickey, other.publickey);
	}

        /*@Override
    public int hashCode() {
        return Objects.hash(id, user, access, name, publickey);
    }*/

}
