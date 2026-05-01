package org.cryptomator.hub.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Vault;

@ApplicationScoped
public class SystemUsageMetrics {

	private static final String VAULTS_METRIC = "hub_vaults";
	private static final String ACTIVE_USERS_METRIC = "hub_active_users";
	private static final String DEVICES_METRIC = "hub_devices";
	private static final AttributeKey<String> DEVICE_TYPE_KEY = AttributeKey.stringKey("type");

	private final Vault.Repository vaultRepo;
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo;
	private final Device.Repository deviceRepo;
	private final ObservableLongGauge vaultsGauge;
	private final ObservableLongGauge activeUsersGauge;
	private final ObservableLongGauge devicesGauge;

	@Inject
	SystemUsageMetrics(Meter meter, Vault.Repository vaultRepo, EffectiveVaultAccess.Repository effectiveVaultAccessRepo, Device.Repository deviceRepo) {
		this.vaultRepo = vaultRepo;
		this.effectiveVaultAccessRepo = effectiveVaultAccessRepo;
		this.deviceRepo = deviceRepo;
		this.vaultsGauge = meter.gaugeBuilder(VAULTS_METRIC)
				.ofLongs()
				.setDescription("Number of vaults")
				.buildWithCallback(this::recordVaultCount);
		this.activeUsersGauge = meter.gaugeBuilder(ACTIVE_USERS_METRIC)
				.ofLongs()
				.setDescription("Number of unique users with access to any non-archived vault")
				.buildWithCallback(this::recordSeatCount);
		this.devicesGauge = meter.gaugeBuilder(DEVICES_METRIC)
				.ofLongs()
				.setDescription("Number of devices grouped by type")
				.buildWithCallback(this::recordDeviceCount);
	}

	private void recordVaultCount(ObservableLongMeasurement measurement) {
		measurement.record(QuarkusTransaction.requiringNew().call(vaultRepo::count));
	}

	private void recordSeatCount(ObservableLongMeasurement measurement) {
		measurement.record(QuarkusTransaction.requiringNew().call(effectiveVaultAccessRepo::countSeatOccupyingUsers));
	}

	private void recordDeviceCount(ObservableLongMeasurement measurement) {
		QuarkusTransaction.requiringNew().run(() -> {
			for (var type : Device.Type.values()) {
				measurement.record(deviceRepo.count("type", type), Attributes.of(DEVICE_TYPE_KEY, type.name()));
			}
		});
	}

	void onStop(@Observes ShutdownEvent event) {
		vaultsGauge.close();
		activeUsersGauge.close();
		devicesGauge.close();
	}

}
