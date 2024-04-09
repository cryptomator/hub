package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "settings")
public class Settings {

	private static final long SINGLETON_ID = 0L;

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	int id;

	@Column(name = "hub_id", nullable = false)
	String hubId;

	@Column(name = "license_key")
	String licenseKey;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHubId() {
		return hubId;
	}

	public void setHubId(String hubId) {
		this.hubId = hubId;
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}

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

	@ApplicationScoped
	public static class Repository implements PanacheRepository<Settings> {

		public Settings get() {
			return Objects.requireNonNull(findById(SINGLETON_ID), "Settings not initialized");
		}
	}
}
