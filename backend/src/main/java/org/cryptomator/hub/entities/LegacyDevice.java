package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Deprecated
@Entity
@Table(name = "device_legacy")
public class LegacyDevice extends PanacheEntityBase {

	@Id
	@Column(name = "id", nullable = false)
	public String id;

	@Column(name = "owner_id", nullable = false)
	public String ownerId;

	// Further attributes omitted, as they are no longer used. The above ones are exceptions, as they are referenced via JPQL for joining.

}
