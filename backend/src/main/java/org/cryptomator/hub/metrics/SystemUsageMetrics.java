package org.cryptomator.hub.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
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
	private static final AttributeKey<String> DEVICE_TYPE_KEY = AttributeKey.stringKey("type");

	@Inject
	Meter meter;

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
		for (var deviceType : Device.Type.values()) {
			devicesPerType.put(deviceType, new AtomicLong(0));
		}

		meter.gaugeBuilder(VAULTS_TOTAL_METRIC)
				.ofLongs()
				.setDescription("Number of vaults")
				.buildWithCallback(measurement -> measurement.record(vaultsTotal.get()));

		meter.gaugeBuilder(ACTIVE_USERS_TOTAL_METRIC)
				.ofLongs()
				.setDescription("Number of unique users with access to any non-archived vault")
				.buildWithCallback(measurement -> measurement.record(activeUsersTotal.get()));

		meter.gaugeBuilder(DEVICES_TOTAL_METRIC)
				.ofLongs()
				.setDescription("Number of devices grouped by type")
				.buildWithCallback(measurement -> {
					for (var entry : devicesPerType.entrySet()) {
						measurement.record(entry.getValue().get(), Attributes.of(DEVICE_TYPE_KEY, entry.getKey().name()));
					}
				});
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
