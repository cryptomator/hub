package org.cryptomator.hub.entities;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "user_details")
public class UserDetails {

	@EmbeddedId
	public Authority.AuthorityId id = new Authority.AuthorityId();

	@OneToOne
	@JoinColumn(name = "id", referencedColumnName = "id")
	@JoinColumn(name = "type", referencedColumnName = "type")
	public Authority authority;

	@Column(name = "picture_url")
	public String pictureUrl;

	@Column(name = "email")
	public String email;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserDetails that = (UserDetails) o;
		return Objects.equals(id, that.id) //
				&& Objects.equals(pictureUrl, that.pictureUrl) //
				&& Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, pictureUrl, email);
	}
}
