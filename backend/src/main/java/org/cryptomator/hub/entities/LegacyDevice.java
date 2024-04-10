package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Deprecated
@Entity
@Table(name = "device_legacy")
public class LegacyDevice {

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "owner_id", nullable = false)
	private String ownerId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	// Further attributes omitted, as they are no longer used. The above ones are exceptions, as they are referenced via JPQL for joining.

	@ApplicationScoped
	@Deprecated
	public static class Repository implements PanacheRepositoryBase<LegacyDevice, String> {
	}
}
