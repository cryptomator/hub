package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "grp")
public class Group extends PanacheEntityBase  {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@Column(name = "name", nullable = false)
	public String name;

	@ManyToMany
	@JoinTable(name = "group_user", joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
	public Set<User> members = new HashSet<>();

	@ManyToMany(mappedBy = "groups")
	public Set<Vault> sharedVaults = new HashSet<>();

	@Override
	public String toString() {
		return "Group{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", members=" + members +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Group group = (Group) o;
		return Objects.equals(id, group.id) && Objects.equals(name, group.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Transactional(Transactional.TxType.REQUIRED)
	public static void createOrUpdate(String id, String name) {
		Group group = findById(id);
		if (group == null) {
			group = new Group();
			group.id = id;
		}
		group.name = name;
		group.persist();
	}

	@Transactional(Transactional.TxType.REQUIRED)
	public static void createOrUpdate(String id, String name, Set<User> users) {
		Group group = findById(id);
		if (group == null) {
			group = new Group();
			group.id = id;
		}
		group.name = name;
		group.members = users;
		group.persist();
	}
}
