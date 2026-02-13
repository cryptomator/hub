package org.cryptomator.hub.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Vault;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class SystemUsageMetrics {

	private static final String VAULTS_TOTAL_METRIC = "hub_vaults_total";
	private static final String ACTIVE_USERS_TOTAL_METRIC = "hub_active_users_total";
	private static final String DEVICES_TOTAL_METRIC = "hub_devices_total";

	@Inject
	MeterRegistry meterRegistry;

	@Inject
	Vault.Repository vaultRepo;

	@Inject
	EffectiveVaultAccess.Repository effectiveVaultAccessRepo;

	@Inject
	Device.Repository deviceRepo;

	private final AtomicLong vaultsTotal = new AtomicLong(0);
	private final AtomicLong activeUsersTotal = new AtomicLong(0);
	private final Map<Device.Type, AtomicLong> devicesPerType = new EnumMap<>(Device.Type.class);

	@PostConstruct
	void registerMetrics() {
		Gauge.builder(VAULTS_TOTAL_METRIC, vaultsTotal, AtomicLong::get)
				.description("Number of vaults")
				.register(meterRegistry);

		Gauge.builder(ACTIVE_USERS_TOTAL_METRIC, activeUsersTotal, AtomicLong::get)
				.description("Number of unique users with access to any non-archived vault")
				.register(meterRegistry);

		for (var deviceType : Device.Type.values()) {
			var value = new AtomicLong(0);
			devicesPerType.put(deviceType, value);
			Gauge.builder(DEVICES_TOTAL_METRIC, value, AtomicLong::get)
					.description("Number of devices grouped by type")
					.tag("type", deviceType.name())
					.register(meterRegistry);
		}
	}

	@Scheduled(every = "24h", delayed = "10s")
	@Transactional
	void collect() {
		vaultsTotal.set(vaultRepo.count());
		activeUsersTotal.set(effectiveVaultAccessRepo.countSeatOccupyingUsers());
		for (var deviceType : Device.Type.values()) {
			var count = deviceRepo.count("type", deviceType);
			devicesPerType.get(deviceType).set(count);
		}
	}
}
