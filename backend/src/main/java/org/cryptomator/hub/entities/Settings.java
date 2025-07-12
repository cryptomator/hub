package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "settings")
public class Settings {

	private static final long SINGLETON_ID = 0L;

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private int id;

	@Column(name = "hub_id", nullable = false)
	private String hubId;

	@Column(name = "license_key")
	private String licenseKey;

	@Column(name = "wot_max_depth", nullable = false)
	private int wotMaxDepth;

	@Column(name = "wot_id_verify_len", nullable = false)
	private int wotIdVerifyLen;

	@Column(name = "default_required_emergency_key_shares", nullable = false)
	private int defaultRequiredEmergencyKeyShares;

	@Column(name = "allow_choosing_emergency_council", nullable = false)
	private boolean allowChoosingEmergencyCouncil;

	@ElementCollection
	@CollectionTable(
			name = "default_emergency_council",
			joinColumns = @JoinColumn(name = "settings_id")
	)
	@Column(name = "member_id")
	private Set<String> emergencyCouncilMemberIds = new HashSet<>();

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

	public int getWotMaxDepth() {
		return wotMaxDepth;
	}

	public void setWotMaxDepth(int wotMaxDepth) {
		this.wotMaxDepth = wotMaxDepth;
	}

	public int getWotIdVerifyLen() {
		return wotIdVerifyLen;
	}

	public void setWotIdVerifyLen(int wotIdVerifyLen) {
		this.wotIdVerifyLen = wotIdVerifyLen;
	}

	public int getDefaultRequiredEmergencyKeyShares() {
		return defaultRequiredEmergencyKeyShares;
	}

	public void setDefaultRequiredEmergencyKeyShares(int defaultRequiredEmergencyKeyShares) {
		this.defaultRequiredEmergencyKeyShares = defaultRequiredEmergencyKeyShares;
	}

	public boolean isAllowChoosingEmergencyCouncil() {
		return allowChoosingEmergencyCouncil;
	}

	public void setAllowChoosingEmergencyCouncil(boolean allowChoosingEmergencyCouncil) {
		this.allowChoosingEmergencyCouncil = allowChoosingEmergencyCouncil;
	}

	public Set<String> getEmergencyCouncilMemberIds() {
		return Set.copyOf(emergencyCouncilMemberIds);
	}

	public void setEmergencyCouncilMemberIds(Collection<String> emergencyCouncilMemberIds) {
		this.emergencyCouncilMemberIds.clear();
		this.emergencyCouncilMemberIds.addAll(emergencyCouncilMemberIds);
	}

	@Override
	public String toString() {
		return "Settings{" +
				"id=" + id +
				", hubId='" + hubId + '\'' +
				", licenseKey='" + licenseKey + '\'' +
				", wotMaxDepth='" + wotMaxDepth + '\'' +
				", wotIdVerifyLen='" + wotIdVerifyLen + '\'' +
				", defaultRequiredEmergencyKeyShares=" + defaultRequiredEmergencyKeyShares +
				", allowChoosingEmergencyCouncil=" + allowChoosingEmergencyCouncil +
				", emergencyCouncilMemberIds= [" + String.join(", ", emergencyCouncilMemberIds) + "]" +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Settings settings = (Settings) o;
		return id == settings.id
				&& Objects.equals(hubId, settings.hubId)
				&& Objects.equals(licenseKey, settings.licenseKey)
				&& Objects.equals(wotMaxDepth, settings.wotMaxDepth)
				&& Objects.equals(wotIdVerifyLen, settings.wotIdVerifyLen)
				&& defaultRequiredEmergencyKeyShares == settings.defaultRequiredEmergencyKeyShares
				&& allowChoosingEmergencyCouncil == settings.allowChoosingEmergencyCouncil
				&& Objects.equals(emergencyCouncilMemberIds, settings.emergencyCouncilMemberIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, hubId, licenseKey, wotMaxDepth, wotIdVerifyLen, defaultRequiredEmergencyKeyShares, allowChoosingEmergencyCouncil, emergencyCouncilMemberIds);
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepository<Settings> {

		public Settings get() {
			return Objects.requireNonNull(findById(SINGLETON_ID), "Settings not initialized");
		}
	}
}
