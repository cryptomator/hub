package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
public class User extends Authority {

	@Column(name = "picture_url")
	private String pictureUrl;

	@Column(name = "email")
	private String email;

	@Column(name = "language")
	private String language;

	@Column(name = "ecdh_publickey")
	private String ecdhPublicKey;

	@Column(name = "ecdsa_publickey")
	private String ecdsaPublicKey;

	@Column(name = "privatekeys")
	private String privateKeys;

	@Column(name = "setupcode")
	private String setupCode;

	public String getPictureUrl() {
		return pictureUrl;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
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

	/**
	 * @deprecated to be removed in <a href="https://github.com/cryptomator/hub/issues/333">#333</a>
	 */
	@Deprecated(since = "1.3.0", forRemoval = true)
	public Set<LegacyDevice> getLegacyDevices() {
		return legacyDevices;
	}

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	public Set<AccessToken> accessTokens = new HashSet<>();

	@OneToMany(mappedBy = "owner", orphanRemoval = true, fetch = FetchType.LAZY)
	public Set<Device> devices = new HashSet<>();

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
				&& Objects.equals(pictureUrl, that.pictureUrl) //
				&& Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), pictureUrl, email);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<User, String> {

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
