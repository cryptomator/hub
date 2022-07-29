package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "authority")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
public class Authority extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Device> devices = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Override
	public String toString() {
		return "Authority{" +
				"id='" + id + '\'' +
				", devices=" + devices.size() +
				", name='" + name + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Authority user = (Authority) o;
		return Objects.equals(id, user.id)
				&& Objects.equals(name, user.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

}
