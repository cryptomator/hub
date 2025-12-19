package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
@NamedQuery(name = "User.getGroupsForUser", query = """
				SELECT DISTINCT g
				FROM Group g
				INNER JOIN EffectiveGroupMembership egm ON g.id = egm.id.groupId
				WHERE egm.id.memberId = :userId
		""")
@NamedQuery(name = "User.countGroupsForUser", query = """
				SELECT count(DISTINCT egm.id.groupId)
				FROM EffectiveGroupMembership egm
				WHERE egm.id.memberId = :userId
		""")
@NamedQuery(name = "User.getVaultsForUser", query = """
				SELECT DISTINCT v
				FROM Vault v
				INNER JOIN EffectiveVaultAccess eva ON v.id = eva.id.vaultId
				WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "User.getVaultAccessForUser", query = """
				SELECT eva
				FROM EffectiveVaultAccess eva
				WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "User.countVaultsForUser", query = """
				SELECT count(DISTINCT eva.id.vaultId)
				FROM EffectiveVaultAccess eva
				WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "User.countDevicesForUser", query = """
				SELECT count(d)
				FROM Device d
				WHERE d.owner.id = :userId
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
	private String[] realmRoles;

	@Column(name = "ecdh_publickey")
	private String ecdhPublicKey;

	@Column(name = "ecdsa_publickey")
	private String ecdsaPublicKey;

	@Column(name = "privatekeys")
	private String privateKeys;

	@Column(name = "setupcode")
	private String setupCode;

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

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	public Set<AccessToken> accessTokens = new HashSet<>();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Device> devices = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "group_membership",
			joinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id")
	)
	public Set<Group> directGroupMemberships = new HashSet<>();

	@Immutable
	@OneToMany(mappedBy = "authority", fetch = FetchType.LAZY)
	public Set<VaultAccess> accessibleVaults = new HashSet<>();

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	public Set<LegacyDevice> getLegacyDevices() {
		return legacyDevices;
	}

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<LegacyDevice> legacyDevices = new HashSet<>();

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

		public User findByIdWithEagerDetails(String id) {
			return find("""
					FROM User u
					LEFT JOIN FETCH u.directGroupMemberships
					LEFT JOIN FETCH u.accessibleVaults
					LEFT JOIN FETCH u.devices
					LEFT JOIN FETCH u.legacyDevices
					WHERE u.id = :id
					""", Parameters.with("id", id)).singleResultOptional().orElse(null);
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

		public Stream<Group> getGroupsForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.getGroupsForUser", Group.class)
					.setParameter("userId", userId)
					.getResultStream();
		}

		public long countGroupsForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.countGroupsForUser", Long.class)
					.setParameter("userId", userId)
					.getSingleResult();
		}

		public Stream<Vault> getVaultsForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.getVaultsForUser", Vault.class)
					.setParameter("userId", userId)
					.getResultStream();
		}

		public Stream<EffectiveVaultAccess> getVaultAccessForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.getVaultAccessForUser", EffectiveVaultAccess.class)
					.setParameter("userId", userId)
					.getResultStream();
		}

		public long countVaultsForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.countVaultsForUser", Long.class)
					.setParameter("userId", userId)
					.getSingleResult();
		}

		public long countDevicesForUser(String userId) {
			return getEntityManager()
					.createNamedQuery("User.countDevicesForUser", Long.class)
					.setParameter("userId", userId)
					.getSingleResult();
		}
	}
}
