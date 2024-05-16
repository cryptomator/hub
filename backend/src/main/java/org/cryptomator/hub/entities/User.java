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

	@Column(name = "publickey")
	private String publicKey;

	@Column(name = "privatekey")
	private String privateKey;

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

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User that = (User) o;
		return super.equals(that) //
				&& Objects.equals(pictureUrl, that.pictureUrl) //
				&& Objects.equals(email, that.email) //
				&& Objects.equals(publicKey, that.publicKey) //
				&& Objects.equals(privateKey, that.privateKey) //
				&& Objects.equals(setupCode, that.setupCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.getId(), pictureUrl, email, publicKey, privateKey, setupCode);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<User, String> {

		public long countEffectiveGroupUsers(String groupdId) {
			return count("#User.countEffectiveGroupUsers", Parameters.with("groupId", groupdId));
		}

		public Stream<User> getEffectiveGroupUsers(String groupdId) {
			return find("#User.getEffectiveGroupUsers", Parameters.with("groupId", groupdId)).stream();
		}
	}
}
