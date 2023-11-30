package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
public class Authority extends PanacheEntityBase { // TODO make sealed?

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@Column(name = "name", nullable = false)
	public String name;

	public static Stream<Authority> byName(String name) {
		return find("#Authority.byName", Parameters.with("name", '%' + name.toLowerCase() + '%')).stream();
	}

	public static Stream<Authority> findAllInList(List<String> ids) {
		return find("#Authority.allInList", Parameters.with("ids", ids)).stream();
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
