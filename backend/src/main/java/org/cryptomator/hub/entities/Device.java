package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Entity
@Table(name = "device")
@NamedQuery(name = "Device.findByIdAndOwner",
		query = "SELECT d FROM Device d WHERE d.id = :deviceId AND d.owner.id = :userId"
)
@NamedQuery(name = "Device.deleteByOwner", query = "DELETE FROM Device d WHERE d.owner.id = :userId")
@NamedQuery(name = "Device.allInList",
		query = """
				SELECT d
				FROM Device d
				WHERE d.id IN :ids
				""")
public class Device {

	public enum Type {
		BROWSER, DESKTOP, MOBILE
	}

	@Id
	@Column(name = "id", nullable = false)
	String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", updatable = false, nullable = false)
	User owner;

	@Column(name = "name", nullable = false)
	String name;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	Type type;

	@Column(name = "publickey", nullable = false)
	String publickey;

	@Column(name = "user_privatekey", nullable = false)
	String userPrivateKey;

	@Column(name = "creation_time", nullable = false)
	Instant creationTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getPublickey() {
		return publickey;
	}

	public void setPublickey(String publickey) {
		this.publickey = publickey;
	}

	public String getUserPrivateKey() {
		return userPrivateKey;
	}

	public void setUserPrivateKey(String userPrivateKey) {
		this.userPrivateKey = userPrivateKey;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	@Override
	public String toString() {
		return "Device{" +
				"id='" + id + '\'' +
				", owner=" + owner.id +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", publickey='" + publickey + '\'' +
				", userPrivateKey='" + userPrivateKey + '\'' +
				", creationTime='" + creationTime + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Device other = (Device) o;
		return Objects.equals(this.id, other.id)
				&& Objects.equals(this.owner, other.owner)
				&& Objects.equals(this.name, other.name)
				&& Objects.equals(this.type, other.type)
				&& Objects.equals(this.publickey, other.publickey)
				&& Objects.equals(this.userPrivateKey, other.userPrivateKey)
				&& Objects.equals(this.creationTime, other.creationTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, owner, name, type, publickey, userPrivateKey, creationTime);
	}

}
