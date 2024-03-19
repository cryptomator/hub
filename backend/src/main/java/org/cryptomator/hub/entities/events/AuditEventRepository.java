package org.cryptomator.hub.entities.events;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.stream.Stream;

@ApplicationScoped
public class AuditEventRepository implements PanacheRepository<AuditEvent> {

	public Stream<AuditEvent> findAllInPeriod(Instant startDate, Instant endDate, long paginationId, boolean ascending, int pageSize) {
		var parameters = Parameters.with("startDate", startDate).and("endDate", endDate).and("paginationId", paginationId);

		final PanacheQuery<AuditEvent> query;
		if (ascending) {
			query = find("#AuditEvent.listAllInPeriodAfterId", parameters);
		} else {
			query = find("#AuditEvent.listAllInPeriodBeforeId", parameters);
		}
		query.page(0, pageSize);
		return query.stream();
	}

	

}
