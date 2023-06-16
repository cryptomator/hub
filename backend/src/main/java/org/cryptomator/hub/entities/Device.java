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
@NamedQuery(name = "Device.allInList",
		query = """
				SELECT d
				FROM Device d
				WHERE d.id IN :ids
				""")
public class Device extends PanacheEntityBase {

	public enum Type {
		BROWSER, DESKTOP, MOBILE
	}

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", updatable = false, nullable = false)
	public User owner;

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	public Type type;

	@Column(name = "publickey", nullable = false)
	public String publickey;

	@Column(name = "user_key", nullable = true)
	public String userKey;

	@Column(name = "creation_time", nullable = false)
	public Instant creationTime;

	@Column(name = "last_seen_time", nullable = false)
	public Instant lastSeenTime;

	@Override
	public String toString() {
		return "Device{" +
				"id='" + id + '\'' +
				", owner=" + owner.id +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", publickey='" + publickey + '\'' +
				", userKey='" + userKey + '\'' +
				", creationTime='" + creationTime + '\'' +
				", lastSeenTime='" + lastSeenTime + '\'' +
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
				&& Objects.equals(this.userKey, other.userKey)
				&& Objects.equals(this.creationTime, other.creationTime)
				&& Objects.equals(this.lastSeenTime, other.lastSeenTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, owner, name, type, publickey, userKey, creationTime, lastSeenTime);
	}

	public static Device findByIdAndUser(String deviceId, String userId) throws NoResultException {
		return find("#Device.findByIdAndOwner", Parameters.with("deviceId", deviceId).and("userId", userId)).singleResult();
	}

	public static Stream<Device> findAllInList(List<String> ids) {
		return find("#Device.allInList", Parameters.with("ids", ids)).stream();
	}
}
