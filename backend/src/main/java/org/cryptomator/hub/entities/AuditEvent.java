package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "audit_event")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NamedQuery(name = "AuditEvent.listAllInPeriod",
		query = """
				SELECT ae
				FROM AuditEvent ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				""")
public class AuditEvent extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	public UUID id;

	@Column(name = "timestamp", nullable = false, updatable = false)
	public Instant timestamp;

	@Column(name = "type", nullable = false, insertable = false, updatable = false)
	public String type;

	@Override
	public String toString() {
		return "AuditEntry{" +
				"id='" + id + '\'' +
				", timestamp=" + timestamp.toString() +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEvent other = (AuditEvent) o;
		return Objects.equals(this.id, other.id)
				&& Objects.equals(this.timestamp, other.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, timestamp);
	}

	public static Stream<AuditEvent> findAllInPeriod(Instant startDate, Instant endDate) {
		return find("#AuditEvent.listAllInPeriod", Parameters.with("startDate", startDate).and("endDate", endDate)).stream();
	}
}
