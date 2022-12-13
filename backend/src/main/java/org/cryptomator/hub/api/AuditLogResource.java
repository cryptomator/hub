package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.AuditEntry;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;

@Path("/auditlog")
public class AuditLogResource {

	@GET
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all auditlog entries within a period", description = "list all auditlog entries from a period specified by a start and end date")
	@Parameter(name = "startDate", description = "the start date of the period in milliseconds from the UNIX epoch", in = ParameterIn.QUERY)
	@Parameter(name = "endDate", description = "the end date of the period in milliseconds from the UNIX epoch", in = ParameterIn.QUERY)
	public List<AuditEntryDto> getAllVaults(@QueryParam("startDate") long startDate, @QueryParam("endDate") long endDate) {
		return AuditEntry.findAllInPeriod(Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate)).map(AuditEntryDto::fromEntity).toList();
	}

	record AuditEntryDto(@JsonProperty("id") String id, @JsonProperty("timestamp") Instant timestamp, @JsonProperty("message") String message) {

		static AuditEntryDto fromEntity(AuditEntry entry) {
			return new AuditEntryDto(entry.id, entry.timestamp.toInstant(), entry.message);
		}
	}
}
