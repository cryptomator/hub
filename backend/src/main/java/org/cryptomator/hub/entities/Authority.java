package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "authority")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NamedQuery(name = "Authority.byName",
		query = """
				SELECT DISTINCT a
				FROM Authority a
				WHERE LOWER(a.name) LIKE :name
				""")
@NamedQuery(name = "Authority.allInList",
		query = """
				SELECT a
				FROM Authority a
				WHERE a.id IN :ids
				""")
public class Authority {

	@Id
	@Column(name = "id", nullable = false)
	String id;

	@Column(name = "name", nullable = false)
	String name;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Authority{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Authority authority = (Authority) o;
		return Objects.equals(id, authority.id)
				&& Objects.equals(name, authority.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

}
