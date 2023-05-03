package org.cryptomator.hub.entities;

import io.quarkus.panache.common.Parameters;
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
					WHERE perm.id.vaultId = :vaultId AND token.vault IS NULL
				"""
)
public class User extends Authority {

	@Column(name = "picture_url")
	public String pictureUrl;

	@Column(name = "email")
	public String email;

	@Column(name = "publickey")
	public String publicKey;

	@Column(name = "privatekey")
	public String privateKey; // IV + GCM-encrypted PKCS#8

	@Column(name = "salt")
	public String salt;

	@Column(name = "iterations")
	public int iterations;

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
				&& Objects.equals(salt, that.salt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, pictureUrl, email, publicKey, privateKey, salt, iterations);
	}

	public static Stream<User> findRequiringAccessGrant(UUID vaultId) {
		return find("#User.requiringAccessGrant", Parameters.with("vaultId", vaultId)).stream();
	}

}
