package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "settings")
public class Settings extends PanacheEntityBase {

	private static final int SINGLETON_ID = 0;

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	public int id;

	@Column(name = "hub_id", nullable = false)
	public String hubId;

	@Column(name = "license_key")
	public String licenseKey;

	@Override
	public String toString() {
		return "Settings{" +
				"id=" + id +
				", hubId='" + hubId + '\'' +
				", licenseKey='" + licenseKey + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Settings settings = (Settings) o;
		return id == settings.id
				&& Objects.equals(hubId, settings.hubId)
				&& Objects.equals(licenseKey, settings.licenseKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, hubId, licenseKey);
	}

	public static Settings get() {
		return Objects.requireNonNull(Settings.findById(SINGLETON_ID), "Settings not initialized");
	}

}
