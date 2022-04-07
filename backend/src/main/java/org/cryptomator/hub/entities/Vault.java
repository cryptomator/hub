package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import org.hibernate.annotations.Immutable;

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
				LEFT JOIN v.effectiveMembers m
				WHERE v.owner.id.id = :userId OR m.id.id = :userId
				""")
public class Vault extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", updatable = false, nullable = false)
	@JoinColumn(name = "owner_type", updatable = false, nullable = false)
	public User owner;

	@ManyToMany
	@JoinTable(name = "vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = {@JoinColumn(name = "authority_id", referencedColumnName = "id"), @JoinColumn(name = "authority_type", referencedColumnName = "type")}
	)
	public Set<Authority> directMembers = new HashSet<>();

	@ManyToMany
	@Immutable
	@JoinTable(name = "effective_vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = {@JoinColumn(name = "authority_id", referencedColumnName = "id"), @JoinColumn(name = "authority_type", referencedColumnName = "type")}
	)
	public Set<Authority> effectiveMembers = new HashSet<>();

	@OneToMany(mappedBy = "vault", fetch = FetchType.LAZY)
	public Set<AccessToken> accessTokens = new HashSet<>(); // rename to accesstokens?

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
				", members=" + directMembers.stream().map(m -> m.id).toList() +
				", accessToken=" + accessTokens.stream().map(a -> a.id).toList() +
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
