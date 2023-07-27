package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.AuditEvent;
import org.cryptomator.hub.entities.AuditEventDeviceRegister;
import org.cryptomator.hub.entities.AuditEventDeviceRemove;
import org.cryptomator.hub.entities.AuditEventVaultAccessGrant;
import org.cryptomator.hub.entities.AuditEventVaultCreate;
import org.cryptomator.hub.entities.AuditEventVaultKeyRetrieve;
import org.cryptomator.hub.entities.AuditEventVaultMemberAdd;
import org.cryptomator.hub.entities.AuditEventVaultMemberRemove;
import org.cryptomator.hub.entities.AuditEventVaultUpdate;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.VaultAccess;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/auditlog")
public class AuditLogResource {

	@GET
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all auditlog entries within a period", description = "list all auditlog entries from a period specified by a start and end date")
	@Parameter(name = "startDate", description = "the start date of the period as ISO 8601 datetime string, inclusive", in = ParameterIn.QUERY)
	@Parameter(name = "endDate", description = "the end date of the period as ISO 8601 datetime string, exclusive", in = ParameterIn.QUERY)
	@Parameter(name = "paginationId", description = "The smallest (asc ordering) or highest (desc ordering) audit entry id, not included in results. Used for pagination. ", in = ParameterIn.QUERY)
	@Parameter(name = "order", description = "The order of the queried table. Determines if most recent (desc) or oldest entries (asc) are considered first. Allowed Values are 'desc' (default) or 'asc'. Used for pagination.", in = ParameterIn.QUERY)
	@Parameter(name = "pageSize", description = "the maximum number of entries to return. Must be between 1 and 100.", in = ParameterIn.QUERY)
	@APIResponse(responseCode = "200", description = "Body contains list of events in the specified time interval")
	@APIResponse(responseCode = "400", description = "startDate or endDate not specified, startDate > endDate, order specified and not in ['asc','desc'] or pageSize not in [1 .. 100]")
	@APIResponse(responseCode = "403", description = "requesting user is does not have admin role")
	public List<AuditEventDto> getAllEvents(@QueryParam("startDate") Instant startDate, @QueryParam("endDate") Instant endDate, @QueryParam("paginationId") Long paginationId, @QueryParam("order") @DefaultValue("desc") String order, @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
		if (startDate == null || endDate == null) {
			throw new BadRequestException("startDate and endDate must be specified");
		} else if (startDate.isAfter(endDate)) {
			throw new BadRequestException("startDate must be before endDate");
		} else if (!(order.equals("desc") || order.equals("asc"))) {
			throw new BadRequestException("order must be either 'asc' or 'desc'");
		} else if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("pageSize must be between 1 and 100");
		} else if (paginationId == null) {
			throw new BadRequestException("paginationId must be specified");
		}

		return AuditEvent.findAllInPeriod(startDate, endDate, paginationId, order.equals("asc"), pageSize).map(AuditEventDto::fromEntity).toList();
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ //
			@JsonSubTypes.Type(value = AuditEventDeviceRegisterDto.class, name = AuditEventDeviceRegister.TYPE), //
			@JsonSubTypes.Type(value = AuditEventDeviceRemoveDto.class, name = AuditEventDeviceRemove.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultCreateDto.class, name = AuditEventVaultCreate.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultUpdateDto.class, name = AuditEventVaultUpdate.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultAccessGrantDto.class, name = AuditEventVaultAccessGrant.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultKeyRetrieveDto.class, name = AuditEventVaultKeyRetrieve.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultMemberAddDto.class, name = AuditEventVaultMemberAdd.TYPE), //
			@JsonSubTypes.Type(value = AuditEventVaultMemberRemoveDto.class, name = AuditEventVaultMemberRemove.TYPE) //
	})
	public interface AuditEventDto {

		@JsonProperty("id")
		long id();

		@JsonProperty("timestamp")
		Instant timestamp();

		static AuditEventDto fromEntity(AuditEvent entity) {
			// TODO: refactor to switch in JDK21
			if (entity instanceof AuditEventDeviceRegister evt) {
				return new AuditEventDeviceRegisterDto(evt.id, evt.timestamp, AuditEventDeviceRegister.TYPE, evt.registeredBy, evt.deviceId, evt.deviceName, evt.deviceType);
			} else if (entity instanceof AuditEventDeviceRemove evt) {
				return new AuditEventDeviceRemoveDto(evt.id, evt.timestamp, AuditEventDeviceRemove.TYPE, evt.removedBy, evt.deviceId);
			} else if (entity instanceof AuditEventVaultCreate evt) {
				return new AuditEventVaultCreateDto(evt.id, evt.timestamp, AuditEventVaultCreate.TYPE, evt.createdBy, evt.vaultId, evt.vaultName, evt.vaultDescription);
			} else if (entity instanceof AuditEventVaultUpdate evt) {
				return new AuditEventVaultUpdateDto(evt.id, evt.timestamp, AuditEventVaultUpdate.TYPE, evt.updatedBy, evt.vaultId, evt.vaultName, evt.vaultDescription, evt.vaultArchived);
			} else if (entity instanceof AuditEventVaultAccessGrant evt) {
				return new AuditEventVaultAccessGrantDto(evt.id, evt.timestamp, AuditEventVaultAccessGrant.TYPE, evt.grantedBy, evt.vaultId, evt.authorityId);
			} else if (entity instanceof AuditEventVaultKeyRetrieve evt) {
				return new AuditEventVaultKeyRetrieveDto(evt.id, evt.timestamp, AuditEventVaultKeyRetrieve.TYPE, evt.retrievedBy, evt.vaultId, evt.result);
			} else if (entity instanceof AuditEventVaultMemberAdd evt) {
				return new AuditEventVaultMemberAddDto(evt.id, evt.timestamp, AuditEventVaultMemberAdd.TYPE, evt.addedBy, evt.vaultId, evt.authorityId, evt.role);
			} else if (entity instanceof AuditEventVaultMemberRemove evt) {
				return new AuditEventVaultMemberRemoveDto(evt.id, evt.timestamp, AuditEventVaultMemberRemove.TYPE, evt.removedBy, evt.vaultId, evt.authorityId);
			} else {
				throw new UnsupportedOperationException("conversion not implemented for event type " + entity.getClass());
			}
		}
	}

	record AuditEventDeviceRegisterDto(long id, Instant timestamp, String type, @JsonProperty("registeredBy") String registeredBy, @JsonProperty("deviceId") String deviceId, @JsonProperty("deviceName") String deviceName,
									   @JsonProperty("deviceType") Device.Type deviceType) implements AuditEventDto {
	}

	record AuditEventDeviceRemoveDto(long id, Instant timestamp, String type, @JsonProperty("removedBy") String removedBy, @JsonProperty("deviceId") String deviceId) implements AuditEventDto {
	}

	record AuditEventVaultCreateDto(long id, Instant timestamp, String type, @JsonProperty("createdBy") String createdBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("vaultName") String vaultName,
									@JsonProperty("vaultDescription") String vaultDescription) implements AuditEventDto {
	}

	record AuditEventVaultUpdateDto(long id, Instant timestamp, String type, @JsonProperty("updatedBy") String updatedBy, @JsonProperty("vaultId") UUID vaultId, @JsonProperty("vaultName") String vaultName,
									@JsonProperty("vaultDescription") String vaultDescription, @JsonProperty("vaultArchived") boolean vaultArchived) implements AuditEventDto {
	}

	record AuditEventVaultAccessGrantDto(long id, Instant timestamp, String type, @JsonProperty("grantedBy") String grantedBy, @JsonProperty("vaultId") UUID vaultId,
										 @JsonProperty("authorityId") String authorityId) implements AuditEventDto {
	}

	record AuditEventVaultKeyRetrieveDto(long id, Instant timestamp, String type, @JsonProperty("retrievedBy") String retrievedBy, @JsonProperty("vaultId") UUID vaultId,
										 @JsonProperty("result") AuditEventVaultKeyRetrieve.Result result) implements AuditEventDto {
	}

	record AuditEventVaultMemberAddDto(long id, Instant timestamp, String type, @JsonProperty("addedBy") String addedBy, @JsonProperty("vaultId") UUID vaultId,
									   @JsonProperty("authorityId") String authorityId, @JsonProperty("role") VaultAccess.Role role) implements AuditEventDto {
	}

	record AuditEventVaultMemberRemoveDto(long id, Instant timestamp, String type, @JsonProperty("removedBy") String removedBy, @JsonProperty("vaultId") UUID vaultId,
										  @JsonProperty("authorityId") String authorityId) implements AuditEventDto {
	}

}
