package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Gatherers;
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
public class Authority {

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "picture_url")
	private String pictureUrl;

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

	public String getPictureUrl() {
		return pictureUrl;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}

	@Override
	public String toString() {
		return "Authority{id='" + id + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Authority authority = (Authority) o;
		return Objects.equals(id, authority.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Authority, String> {

		public Stream<Authority> byName(String name) {
			return find("#Authority.byName", Map.of("name", '%' + name.toLowerCase() + '%')).stream();
		}

		public Stream<Authority> findAllInList(Collection<String> ids) {
			return ids.stream()
					.gather(Gatherers.windowFixed(200))
					.flatMap(batch -> find("WHERE id IN :ids", Map.of("ids", batch)).stream());
		}
	}
}
