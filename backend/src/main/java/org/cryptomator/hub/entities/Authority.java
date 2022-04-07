package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "authority")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
public class Authority extends PanacheEntityBase {

	@EmbeddedId
	public AuthorityId id = new AuthorityId();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Device> devices = new HashSet<>();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Vault> ownedVaults = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", devices=" + devices.size() +
				", ownedVaults=" + ownedVaults.size() +
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

	public enum AuthorityType {USER, GROUP}

	@Embeddable
	public static class AuthorityId implements Serializable {

		public String id;
		@Enumerated(EnumType.STRING)
		public AuthorityType type;

		public AuthorityId() {
		}

		public AuthorityId(String id, AuthorityType type) {
			this.id = id;
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AuthorityId that = (AuthorityId) o;
			return Objects.equals(id, that.id) && type == that.type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, type);
		}

		@Override
		public String toString() {
			return "AuthorityId{id='" + id + "', type=" + type + '}';
		}
	}

}
