package org.cryptomator.hub.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum RealmRole {
	@JsonProperty("user") USER("user"),
	@JsonProperty("admin") ADMIN("admin"),
	@JsonProperty("create-vaults") CREATE_VAULTS("create-vaults");

	private final String kcName;

	RealmRole(String kcName) {
		this.kcName = kcName;
	}

	public String kcName() {
		return kcName;
	}

	public static RealmRole fromKcName(String kcName) {
		for (RealmRole role : values()) {
			if (role.kcName.equals(kcName)) {
				return role;
			}
		}
		throw new IllegalArgumentException("No matching RealmRole for Keycloak name: " + kcName);
	}

	public static Set<RealmRole> fromKcNames(Collection<String> kcNames) {
		if (kcNames == null || kcNames.isEmpty()) {
			return EnumSet.noneOf(RealmRole.class);
		}
		Set<RealmRole> roles = EnumSet.noneOf(RealmRole.class);
		for (String kcName : kcNames) {
			for (RealmRole role : values()) {
				if (role.kcName.equals(kcName)) {
					roles.add(role);
				}
			}
		}
		return roles;
	}
}
