package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "vault")
@NamedQuery(name = "Vault.accessibleByUser",
		query = """
				SELECT DISTINCT v
				FROM Vault v
				LEFT JOIN v.effectiveMembers m
				WHERE m.id = :userId
				""")
public class Vault extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public UUID id;

	@ManyToMany
	@Immutable
	@JoinTable(name = "vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
	)
	public Set<Authority> directMembers = new HashSet<>();

	@ManyToMany
	@Immutable
	@JoinTable(name = "effective_vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
	)
	public Set<Authority> effectiveMembers = new HashSet<>();

	@OneToMany(mappedBy = "vault", fetch = FetchType.LAZY)
	public Set<AccessToken> accessTokens = new HashSet<>();

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "salt", nullable = false)
	public String salt;

	@Column(name = "iterations", nullable = false)
	public int iterations;

	@Column(name = "masterkey", nullable = false)
	public String masterkey;

	@Column(name = "auth_pubkey", nullable = false)
	public String authenticationPublicKey;

	@Column(name = "auth_prvkey", nullable = false)
	public String authenticationPrivateKey;

	@Column(name = "creation_time", nullable = false)
	public Instant creationTime;

	@Column(name = "description")
	public String description;

	public static Stream<Vault> findAccessibleByUser(String userId) {
		return find("#Vault.accessibleByUser", Parameters.with("userId", userId)).stream();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vault vault = (Vault) o;
		return Objects.equals(id, vault.id)
				&& Objects.equals(name, vault.name)
				&& Objects.equals(salt, vault.salt)
				&& Objects.equals(iterations, vault.iterations)
				&& Objects.equals(masterkey, vault.masterkey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, salt, iterations, masterkey);
	}

	@Override
	public String toString() {
		return "Vault{" +
				"id='" + id + '\'' +
				", members=" + directMembers.stream().map(m -> m.id).collect(Collectors.joining(", ")) +
				", accessToken=" + accessTokens.stream().map(a -> a.id.toString()).collect(Collectors.joining(", ")) +
				", name='" + name + '\'' +
				", salt='" + salt + '\'' +
				", iterations='" + iterations + '\'' +
				", masterkey='" + masterkey + '\'' +
				", authenticationPublicKey='" + authenticationPublicKey + '\'' +
				", authenticationPrivateKey='" + authenticationPrivateKey + '\'' +
				'}';
	}

}
