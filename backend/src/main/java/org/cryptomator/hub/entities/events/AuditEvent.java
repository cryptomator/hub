package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
				AND ae.id < :paginationId
				AND (:allTypes = true OR ae.type IN :types)
				ORDER BY ae.id DESC
				""")
@NamedQuery(name = "AuditEvent.listAllInPeriodAfterId",
		query = """
				SELECT ae
				FROM AuditEvent ae
				WHERE ae.timestamp >= :startDate
				AND ae.timestamp < :endDate
				AND ae.id > :paginationId
				AND (:allTypes = true OR ae.type IN :types)
				ORDER BY ae.id ASC
				""")
@NamedQuery(name = "AuditEvent.lastVaultKeyRetrieve",
		query = """
				SELECT e1
				FROM VaultKeyRetrievedEvent e1
				WHERE e1.deviceId IN (:deviceIds)
				AND e1.timestamp = (
					SELECT MAX(e2.timestamp)
					FROM VaultKeyRetrievedEvent e2
					WHERE e2.deviceId = e1.deviceId
				  )
				""")
@SequenceGenerator(name = "audit_event_id_seq", sequenceName = "audit_event_id_seq", allocationSize = 1)
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_event_id_seq")
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	@Column(name = "timestamp", nullable = false, updatable = false)
	private Instant timestamp;

	@Column(name = "type", nullable = false, insertable = false, updatable = false)
	private String type;

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


	@ApplicationScoped
	public static class Repository implements PanacheRepository<AuditEvent> {

		public Stream<AuditEvent> findAllInPeriod(Instant startDate, Instant endDate, List<String> type, long paginationId, boolean ascending, int pageSize) {
			var allTypes = type.isEmpty();

			var parameters = Parameters.with("startDate", startDate)
					.and("endDate", endDate)
					.and("paginationId", paginationId)
					.and("types", type)
					.and("allTypes", allTypes);

			final PanacheQuery<AuditEvent> query;
			if (ascending) {
				query = find("#AuditEvent.listAllInPeriodAfterId", parameters);
			} else {
				query = find("#AuditEvent.listAllInPeriodBeforeId", parameters);
			}
			query.page(0, pageSize);
			return query.stream();
		}

		public Stream<VaultKeyRetrievedEvent> findLastVaultKeyRetrieve(Set<String> deviceIds) {
			return find("#AuditEvent.lastVaultKeyRetrieve", Parameters.with("deviceIds", deviceIds)).stream();
		}
	}
}
