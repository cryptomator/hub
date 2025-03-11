package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.Instant;

@NamedQuery(name = "LegacyDevice.deleteByOwner", query = "DELETE FROM LegacyDevice d WHERE d.owner.id = :userId")

@Deprecated
@Entity
@Table(name = "device_legacy")
public class LegacyDevice {

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", updatable = false, nullable = false)
	private User owner;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private Device.Type type;

	@Column(name = "creation_time", nullable = false)
	private Instant creationTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public Device.Type getType() {
		return type;
	}

	// Further attributes omitted, as they are no longer used. The above ones are exceptions, as they are referenced via JPQL for joining and required for the device list.

	@ApplicationScoped
	@Deprecated
	public static class Repository implements PanacheRepositoryBase<LegacyDevice, String> {

		public void deleteByOwner(String userId) {
			delete("#LegacyDevice.deleteByOwner", Parameters.with("userId", userId));
		}
	}
}
