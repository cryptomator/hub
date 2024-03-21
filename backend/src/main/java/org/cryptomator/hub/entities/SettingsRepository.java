package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

import static org.cryptomator.hub.entities.Settings.SINGLETON_ID;

@ApplicationScoped
public class SettingsRepository implements PanacheRepository<Settings> {

	public Settings get() {
		return Objects.requireNonNull(findById((long) SINGLETON_ID), "Settings not initialized");
	}
}
