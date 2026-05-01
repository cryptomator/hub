package org.cryptomator.hub.metrics;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VaultUnlockMetrics {

	private static final String UNLOCKS_TOTAL_METRIC = "hub_vault_unlocks_total";
	private static final String LAST_SUCCESS_METRIC = "hub_vault_unlock_last_success_epoch_seconds";
	private static final String LAST_FAILURE_METRIC = "hub_vault_unlock_last_failure_epoch_seconds";

	private final LongCounter unlockCounter;
	private final LongGauge lastSuccessGauge;
	private final LongGauge lastFailureGauge;

	@Inject
	VaultUnlockMetrics(Meter meter) {
		this.unlockCounter = meter.counterBuilder(UNLOCKS_TOTAL_METRIC)
				.setDescription("Total number of vault unlock attempts")
				.build();
		this.lastSuccessGauge = meter.gaugeBuilder(LAST_SUCCESS_METRIC)
				.ofLongs()
				.setDescription("Epoch timestamp in seconds of the last successful vault unlock")
				.build();
		this.lastFailureGauge = meter.gaugeBuilder(LAST_FAILURE_METRIC)
				.ofLongs()
				.setDescription("Epoch timestamp in seconds of the last failed vault unlock")
				.build();
	}

	public void recordUnlock() {
		unlockCounter.add(1);
	}

	public void recordSuccess() {
		lastSuccessGauge.set(currentEpochSeconds());
	}

	public void recordFailure() {
		lastFailureGauge.set(currentEpochSeconds());
	}

	private long currentEpochSeconds() {
		return System.currentTimeMillis() / 1000;
	}
}
