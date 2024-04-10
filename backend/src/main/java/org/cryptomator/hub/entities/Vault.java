package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
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

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
				INNER JOIN EffectiveVaultAccess a ON a.id.vaultId = v.id AND a.id.authorityId = :userId
				""")
@NamedQuery(name = "Vault.accessibleByUserAndRole",
		query = """
				SELECT DISTINCT v
				FROM Vault v
				INNER JOIN EffectiveVaultAccess a ON a.id.vaultId = v.id AND a.id.authorityId = :userId
				WHERE a.id.role = :role
				""")
@NamedQuery(name = "Vault.allInList",
		query = """
				SELECT v
				FROM Vault v
				WHERE v.id IN :ids
				"""
)
public class Vault {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@ManyToMany
	@Immutable
	@JoinTable(name = "vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
	)
	private Set<Authority> directMembers = new HashSet<>();

	@ManyToMany
	@Immutable
	@JoinTable(name = "effective_vault_access",
			joinColumns = @JoinColumn(name = "vault_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
	)
	private Set<Authority> effectiveMembers = new HashSet<>();

	@OneToMany(mappedBy = "vault", fetch = FetchType.LAZY)
	private Set<AccessToken> accessTokens = new HashSet<>();

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "salt")
	private String salt;

	@Column(name = "iterations")
	private Integer iterations;

	@Column(name = "masterkey")
	private String masterkey;

	@Column(name = "auth_pubkey")
	private String authenticationPublicKey;

	@Column(name = "auth_prvkey")
	private String authenticationPrivateKey;

	@Column(name = "creation_time", nullable = false)
	private Instant creationTime;

	@Column(name = "description")
	private String description;

	@Column(name = "archived", nullable = false)
	private boolean archived;

	public Optional<ECPublicKey> getAuthenticationPublicKeyOptional() {
		if (authenticationPublicKey == null) {
			return Optional.empty();
		}

		try {
			var publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(authenticationPublicKey));
			var keyFactory = KeyFactory.getInstance("EC");
			var key = keyFactory.generatePublic(publicKeySpec);
			if (key instanceof ECPublicKey k) {
				return Optional.of(k);
			} else {
				return Optional.empty();
			}
		} catch (InvalidKeySpecException e) {
			return Optional.empty();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Set<Authority> getDirectMembers() {
		return directMembers;
	}

	public void setDirectMembers(Set<Authority> directMembers) {
		this.directMembers = directMembers;
	}

	public Set<Authority> getEffectiveMembers() {
		return effectiveMembers;
	}

	public void setEffectiveMembers(Set<Authority> effectiveMembers) {
		this.effectiveMembers = effectiveMembers;
	}

	public Set<AccessToken> getAccessTokens() {
		return accessTokens;
	}

	public void setAccessTokens(Set<AccessToken> accessTokens) {
		this.accessTokens = accessTokens;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public Integer getIterations() {
		return iterations;
	}

	public void setIterations(Integer iterations) {
		this.iterations = iterations;
	}

	public String getMasterkey() {
		return masterkey;
	}

	public void setMasterkey(String masterkey) {
		this.masterkey = masterkey;
	}

	public void setAuthenticationPublicKey(String authenticationPublicKey) {
		this.authenticationPublicKey = authenticationPublicKey;
	}

	public String getAuthenticationPrivateKey() {
		return authenticationPrivateKey;
	}

	public String getAuthenticationPublicKey() {
		return authenticationPublicKey;
	}

	public void setAuthenticationPrivateKey(String authenticationPrivateKey) {
		this.authenticationPrivateKey = authenticationPrivateKey;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
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
				&& Objects.equals(masterkey, vault.masterkey)
				&& Objects.equals(archived, vault.archived);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, salt, iterations, masterkey, archived);
	}

	@Override
	public String toString() {
		return "Vault{" +
				"id='" + id + '\'' +
				", members=" + directMembers.stream().map(Authority::getId).collect(Collectors.joining(", ")) +
				", accessToken=" + accessTokens.stream().map(a -> a.getId().toString()).collect(Collectors.joining(", ")) +
				", name='" + name + '\'' +
				", archived='" + archived + '\'' +
				", salt='" + salt + '\'' +
				", iterations='" + iterations + '\'' +
				", masterkey='" + masterkey + '\'' +
				", authenticationPublicKey='" + authenticationPublicKey + '\'' +
				", authenticationPrivateKey='" + authenticationPrivateKey + '\'' +
				'}';
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Vault, UUID> {

		public Stream<Vault> findAccessibleByUser(String userId) {
			return find("#Vault.accessibleByUser", Parameters.with("userId", userId)).stream();
		}

		public Stream<Vault> findAccessibleByUser(String userId, VaultAccess.Role role) {
			return find("#Vault.accessibleByUserAndRole", Parameters.with("userId", userId).and("role", role)).stream();
		}

		public Stream<Vault> findAllInList(List<UUID> ids) {
			return find("#Vault.allInList", Parameters.with("ids", ids)).stream();
		}
	}
}
