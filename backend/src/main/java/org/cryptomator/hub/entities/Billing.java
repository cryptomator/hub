package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "billing")
public class Billing extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public int id;

	@Column(name = "hub_id", nullable = false)
	public String hubId;

	@Column(name = "token")
	public String token;

	@Override
	public String toString() {
		return "Billing{" +
				"id=" + id +
				", hubId='" + hubId + '\'' +
				", token='" + token + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Billing billing = (Billing) o;
		return id == billing.id
				&& Objects.equals(hubId, billing.hubId)
				&& Objects.equals(token, billing.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, hubId, token);
	}

}
