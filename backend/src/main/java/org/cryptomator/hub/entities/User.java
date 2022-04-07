package org.cryptomator.hub.entities;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "user_details")
@DiscriminatorValue("USER")
public class User extends Authority {

	@Column(name = "picture_url")
	public String pictureUrl;

	@Column(name = "email")
	public String email;

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

	// --- data layer queries ---

	@Transactional(Transactional.TxType.REQUIRED)
	public static void createOrUpdate(String id, String name, String pictureUrl, String email) {
		User user = findById(id);
		if (user == null) {
			user = new User();
			user.id.id = id;
			user.id.type = AuthorityType.USER;
		}
		user.name = name;
		user.pictureUrl = pictureUrl;
		user.email = email;
		user.persist();
	}

	public static User findById(String id) {
		var compositeId = new AuthorityId(id, AuthorityType.USER);
		return Authority.findById(compositeId);
	}

	public static Optional<User> findByIdOptional(String id) {
		var compositeId = new AuthorityId(id, AuthorityType.USER);
		return Authority.findByIdOptional(compositeId);
	}
}
