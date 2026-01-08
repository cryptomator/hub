package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "user_details")
@DiscriminatorValue("USER")
@NamedQuery(name = "User.requiringAccessGrant",
		query = """
				SELECT u
				FROM User u
					INNER JOIN EffectiveVaultAccess perm ON u.id = perm.id.authorityId
					LEFT JOIN u.accessTokens token ON token.id.vaultId = :vaultId AND token.id.userId = u.id
					WHERE perm.id.vaultId = :vaultId AND token.vault IS NULL AND u.ecdhPublicKey IS NOT NULL
				"""
)
@NamedQuery(name = "User.getEffectiveGroupUsers", query = """
				SELECT DISTINCT u
				FROM User u
				INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
				WHERE egm.id.groupId = :groupId
		""")
@NamedQuery(name = "User.countEffectiveGroupUsers", query = """
				SELECT count( DISTINCT u)
				FROM User u
				INNER JOIN EffectiveGroupMembership egm	ON u.id = egm.id.memberId
				WHERE egm.id.groupId = :groupId
		""")
public class User extends Authority {

	@Column(name = "email")
	private String email;

	@Column(name = "firstname")
	private String firstName;

	@Column(name = "lastname")
	private String lastName;

	@Column(name = "language")
	private String language;

	@Column(name = "realm_roles")
	@Type(StringArrayType.class)
	private String[] realmRoles = new String[0];

	@Column(name = "ecdh_publickey")
	private String ecdhPublicKey;

	@Column(name = "ecdsa_publickey")
	private String ecdsaPublicKey;

	@Column(name = "privatekeys")
	private String privateKeys;

	@Column(name = "setupcode")
	private String setupCode;

	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	public UserMetrics metrics;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private Set<AccessToken> accessTokens = new HashSet<>();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<Device> devices = new HashSet<>();

	@ManyToMany(mappedBy = "members", cascade = {})
	private Set<Group> directGroupMemberships = new HashSet<>();

	@Immutable
	@OneToMany(mappedBy = "authority", fetch = FetchType.LAZY)
	private Set<EffectiveVaultAccess> accessibleVaults = new HashSet<>();

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<LegacyDevice> legacyDevices = new HashSet<>();

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String[] getRealmRoles() {
		return realmRoles;
	}

	public void setRealmRoles(String[] realmRoles) {
		this.realmRoles = realmRoles;
	}

	public String getEcdhPublicKey() {
		return ecdhPublicKey;
	}

	public void setEcdhPublicKey(String ecdhPublicKey) {
		this.ecdhPublicKey = ecdhPublicKey;
	}

	public String getEcdsaPublicKey() {
		return ecdsaPublicKey;
	}

	public void setEcdsaPublicKey(String ecdsaPublicKey) {
		this.ecdsaPublicKey = ecdsaPublicKey;
	}

	public String getPrivateKeys() {
		return privateKeys;
	}

	public void setPrivateKeys(String privateKeys) {
		this.privateKeys = privateKeys;
	}

	public String getSetupCode() {
		return setupCode;
	}

	public void setSetupCode(String setupCode) {
		this.setupCode = setupCode;
	}

	public Set<AccessToken> getAccessTokens() {
		return accessTokens;
	}

	public void setAccessTokens(Set<AccessToken> accessTokens) {
		this.accessTokens = accessTokens;
	}

	public Set<Device> getDevices() {
		return devices;
	}

	public void setDevices(Set<Device> devices) {
		this.devices = devices;
	}

	public UserMetrics getMetrics() {
		return metrics;
	}

	public Set<Group> getDirectGroupMemberships() {
		return directGroupMemberships;
	}

	public Set<EffectiveVaultAccess> getAccessibleVaults() {
		return accessibleVaults;
	}

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	public Set<LegacyDevice> getLegacyDevices() {
		return legacyDevices;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User that = (User) o;
		return super.equals(that) //
				&& Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), email);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<User, String> {

		public PanacheQuery<User> findAllWithMetrics() {
			return find("""
					FROM User u
					LEFT JOIN FETCH u.metrics m
					""");
		}

		public User findByIdWithEagerDetails(String id) {
			// 1. fetch user with groups, devices, legacy devices:
			// we can do this in a single query since we don't expect a large number of groups/devices per user,
			// e.g. 1 user x 5 groups x 5 devices = 25 rows, which is acceptable
			var user = find("""
					FROM User u
					LEFT JOIN FETCH u.directGroupMemberships dgm
					LEFT JOIN FETCH u.devices d
					LEFT JOIN FETCH u.legacyDevices ld
					WHERE u.id = :id
					""", Parameters.with("id", id)).singleResultOptional().orElse(null);
			if (user == null) {
				return null;
			}
			// 2. fetch accessible vaults separately to avoid cartesian product explosion:
			// we replace the persistent set with our own set (allowed because it's marked as @Immutable)
			user.accessibleVaults = getEntityManager().createQuery("""
							SELECT DISTINCT eva
							FROM EffectiveVaultAccess eva
							INNER JOIN FETCH eva.vault v
							WHERE eva.authority.id = :userId
							""", EffectiveVaultAccess.class)
					.setParameter("userId", id)
					.getResultStream().collect(Collectors.toSet());
			return user;
		}

		public Stream<User> findByIds(Collection<String> ids) {
			return Batch.of(200).run(ids, Stream.empty(), (batch, result) -> {
				var partial = find("id IN :ids", Parameters.with("ids", batch));
				return Stream.concat(result, partial.stream());
			});
		}

		public long deleteByIds(Collection<String> ids) {
			return Batch.of(200).run(ids, 0L, (batch, result) -> result + delete("id IN :ids", Parameters.with("ids", batch)));
		}

		public Stream<User> findRequiringAccessGrant(UUID vaultId) {
			return find("#User.requiringAccessGrant", Parameters.with("vaultId", vaultId)).stream();
		}

		public long countEffectiveGroupUsers(String groupdId) {
			return count("#User.countEffectiveGroupUsers", Parameters.with("groupId", groupdId));
		}

		public Stream<User> getEffectiveGroupUsers(String groupdId) {
			return find("#User.getEffectiveGroupUsers", Parameters.with("groupId", groupdId)).stream();
		}

	}
}
