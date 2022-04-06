package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
public class Authority extends PanacheEntityBase {

	@EmbeddedId
	public AuthorityId id = new AuthorityId();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Device> devices = new HashSet<>();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Vault> ownedVaults = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@OneToOne(mappedBy = "authority", cascade = {CascadeType.ALL})
	public UserDetails details;

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
				&& Objects.equals(name, user.name)
				&& Objects.equals(details, user.details);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, details);
	}

	public enum AuthorityType {USER, GROUP}

	@Embeddable
	public static class AuthorityId implements Serializable {

		@Column(name = "id", nullable = false)
		private String id;

		@Enumerated(EnumType.STRING)
		@Column(name = "type", length = 5, nullable = false)
		private AuthorityType type;

		public AuthorityId() {
		}

		public AuthorityId(String id, AuthorityType type) {
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return id;
		}

		public AuthorityType getType() {
			return type;
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

	// --- data layer queries ---

	@Transactional(Transactional.TxType.REQUIRED)
	public static void createOrUpdate(String id, String name, String pictureUrl, String email) {
		var compositeId = new AuthorityId(id, AuthorityType.USER);
		Authority user = findById(compositeId);
		if (user == null) {
			user = new Authority();
			user.id = compositeId;
		}
		user.name = name;
		user.details = new UserDetails();
		user.details.pictureUrl = pictureUrl;
		user.details.email = email;
		user.persist();
	}
}
