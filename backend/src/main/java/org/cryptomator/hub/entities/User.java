package org.cryptomator.hub.entities;

import io.quarkus.panache.common.Parameters;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "user_details")
@DiscriminatorValue("USER")
@NamedQuery(name = "User.countEVUs", query = """
  		SELECT count( DISTINCT u)
  		FROM User u
  		INNER JOIN EffectiveVaultAccess eva	ON u.id = eva.id.authorityId
		""")
@NamedQuery(name = "User.countEGUs", query = """
  		SELECT count( DISTINCT u)
  		FROM User u
  		INNER JOIN EffectiveGroupMembership egm	ON u.id = egm.id.memberId
  		WHERE egm.id.groupId = :groupId
		""")
public class User extends Authority {

	@Column(name = "picture_url")
	public String pictureUrl;

	@Column(name = "email")
	public String email;

	public static long countEffectiveVaultUsers() {
		return User.count("#User.countEVUs");
	}

	public static long countEffectiveGroupUsers(String groupdId) {
		return User.count("#User.countEGUs", Parameters.with("groupId", groupdId));
	}

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
		return Objects.hash(id, pictureUrl, email);
	}

}
