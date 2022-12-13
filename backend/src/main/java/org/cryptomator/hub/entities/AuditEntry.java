package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "audit_log")
@NamedQuery(name = "AuditEntry.listAllInPeriod",
		query = """
				SELECT ae
				FROM AuditEntry ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				""")
public class AuditEntry extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	public String id;

	@Column(name = "timestamp", nullable = false, updatable = false)
	public Timestamp timestamp;

	@Column(name = "message", nullable = false, updatable = false)
	public String message;


	@Override
	public String toString() {
		return "AuditEntry{" +
				"id='" + id + '\'' +
				", timestamp=" + timestamp.toString() +
				", message='" + message + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEntry other = (AuditEntry) o;
		return Objects.equals(this.id, other.id)
				&& Objects.equals(this.timestamp, other.timestamp)
				&& Objects.equals(this.message, other.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, timestamp, message);
	}

	public static void log(String msg) {
		if(msg.length() > 2000){
			throw new IllegalArgumentException("Audit message can only be 2000 chars long");
		}
		var entry = new AuditEntry();
		entry.id = UUID.randomUUID().toString();
		entry.timestamp = Timestamp.from(Instant.now());
		entry.message = msg;
		entry.persist();
	}

	public static Stream<AuditEntry> findAllInPeriod(Instant startDate, Instant endDate) {
		return find("#AuditEntry.listAllInPeriod", Parameters.with("startDate", Timestamp.from(startDate)).and("endDate", Timestamp.from(endDate))).stream();
	}
}
