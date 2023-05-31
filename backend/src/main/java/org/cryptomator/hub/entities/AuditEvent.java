package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

@Entity
@Table(name = "audit_event")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NamedQuery(name = "AuditEvent.listAllInPeriodBeforeId",
		query = """
				SELECT ae
				FROM AuditEvent ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				AND ae.id < :beforeId
				ORDER BY ae.id DESC
				""")
@NamedQuery(name = "AuditEvent.listAllInPeriodAfterId",
		query = """
				SELECT ae
				FROM AuditEvent ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				AND ae.id > :afterId
				ORDER BY ae.id ASC
				""")
@SequenceGenerator(name = "audit_event_id_seq", sequenceName = "audit_event_id_seq")
public class AuditEvent extends PanacheEntityBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_event_id_seq")
	@Column(name = "id", nullable = false, updatable = false)
	public long id;

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

	public static Stream<AuditEvent> findAllInPeriodBeforeId(Instant startDate, Instant endDate, long beforeId, int pageSize) {
		var query = find("#AuditEvent.listAllInPeriodBeforeId", Parameters.with("startDate", startDate).and("endDate", endDate).and("beforeId", beforeId));
		query.page(0, pageSize);
		return query.stream();
	}

	public static Stream<AuditEvent> findAllInPeriodAfterId(Instant startDate, Instant endDate, long afterId, int pageSize) {
		var query = find("#AuditEvent.listAllInPeriodAfterId", Parameters.with("startDate", startDate).and("endDate", endDate).and("afterId", afterId));
		query.page(0, pageSize);
		return query.stream();
	}
}
