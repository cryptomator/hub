package org.cryptomator.hub.entities.events;

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
				AND ae.id < :paginationId
				ORDER BY ae.id DESC
				""")
@NamedQuery(name = "AuditEvent.listAllInPeriodAfterId",
		query = """
				SELECT ae
				FROM AuditEvent ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				AND ae.id > :paginationId
				ORDER BY ae.id ASC
				""")
@SequenceGenerator(name = "audit_event_id_seq", sequenceName = "audit_event_id_seq", allocationSize = 1)
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_event_id_seq")
	@Column(name = "id", nullable = false, updatable = false)
	long id;

	@Column(name = "timestamp", nullable = false, updatable = false)
	Instant timestamp;

	@Column(name = "type", nullable = false, insertable = false, updatable = false)
	String type;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

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

}