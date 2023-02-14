package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.cryptomator.hub.entities.AuditEvent;
import org.cryptomator.hub.entities.UnlockEvent;
import org.cryptomator.hub.entities.UnlockResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/auditlog")
public class AuditLogResource {

	@GET
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all auditlog entries within a period", description = "list all auditlog entries from a period specified by a start and end date")
	@Parameter(name = "startDate", description = "the start date of the period in milliseconds from the UNIX epoch", in = ParameterIn.QUERY)
	@Parameter(name = "endDate", description = "the end date of the period in milliseconds from the UNIX epoch", in = ParameterIn.QUERY)
	public EventList getAllEvents(@QueryParam("startDate") long startDate, @QueryParam("endDate") long endDate) {
		var events = AuditEvent.findAllInPeriod(Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate)).map(AuditEventDto::fromEntity).toList();
		return new EventList(events);
	}

	// Helper class to prevent type erasure for @JsonTypeInfo, see https://github.com/FasterXML/jackson-databind/issues/336
	public static class EventList extends ArrayList<AuditEventDto> {
		EventList(List<AuditEventDto> events) {
			super(events);
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = UnlockEventDto.class, name = UnlockEvent.TYPE)
	})
	public interface AuditEventDto {

		@JsonProperty("id")
		UUID id();

		@JsonProperty("timestamp")
		Instant timestamp();

		static AuditEventDto fromEntity(AuditEvent entity) {
			return switch (entity) {
				case UnlockEvent e -> new UnlockEventDto(e.id, e.timestamp.toInstant(), UnlockEvent.TYPE, e.userId, e.vaultId, e.deviceId, e.result);
				default -> throw new UnsupportedOperationException("conversion not implemented for event type " + entity.getClass());
			};
		}
	}

	record UnlockEventDto(UUID id, Instant timestamp, String type, @JsonProperty("userId") String userId, @JsonProperty("vaultId") String vaultId, @JsonProperty("deviceId") String deviceId, @JsonProperty("result") UnlockResult result) implements AuditEventDto {
	}

}
