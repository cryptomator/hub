package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Entity
@Table(name = "vault")
@NamedQuery(name = "Vault.accessibleOrOwnedByUser",
		query = """
				SELECT DISTINCT v
				FROM Vault v
				LEFT JOIN v.accesses va
				LEFT JOIN va.device vad
				LEFT JOIN v.groups vg
				LEFT JOIN vg.members vgm
				LEFT JOIN vgm.devices vgmd
				WHERE v.owner.id = :userId OR vad.owner.id = :userId OR vgmd.owner.id = :userId
				""")
public class Vault extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", updatable = false, nullable = false)
	public User owner;

	@ManyToMany
	@JoinTable(name = "vault_user", joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
	public Set<User> members = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "vault_group", joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"))
	public Set<Group> groups = new HashSet<>();

	@OneToMany(mappedBy = "vault", fetch = FetchType.LAZY)
	public Set<Access> accesses = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "salt", nullable = false)
	public String salt;

	@Column(name = "iterations", nullable = false)
	public String iterations;

	@Column(name = "masterkey", nullable = false)
	public String masterkey;

	@Column(name = "creation_time", nullable = false)
	public Timestamp creationTime;

	@Column(name = "description")
	public String description;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vault vault = (Vault) o;
		return Objects.equals(id, vault.id)
				&& Objects.equals(owner, vault.owner)
				&& Objects.equals(name, vault.name)
				&& Objects.equals(salt, vault.salt)
				&& Objects.equals(iterations, vault.iterations)
				&& Objects.equals(masterkey, vault.masterkey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, owner, name, salt, iterations, masterkey);
	}

	@Override
	public String toString() {
		return "Vault{" +
				"id='" + id + '\'' +
				", owner=" + owner +
				", members=" + members.stream().map(m -> m.id).toList() +
				", groups=" + groups.stream().map(g -> g.id).toList() +
				", accesses=" + accesses.stream().map(a -> a.id).toList() +
				", name='" + name + '\'' +
				", salt='" + salt + '\'' +
				", iterations='" + iterations + '\'' +
				", masterkey='" + masterkey + '\'' +
				'}';
	}

	public static Stream<Vault> findAccessibleOrOwnerByUser(String userId) {
		return find("#Vault.accessibleOrOwnedByUser", Parameters.with("userId", userId)).stream();
	}

}
