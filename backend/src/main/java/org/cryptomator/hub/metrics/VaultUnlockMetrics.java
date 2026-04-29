package org.cryptomator.hub.metrics;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class VaultUnlockMetrics {

	private static final String UNLOCKS_TOTAL_METRIC = "hub_vault_unlocks_total";
	private static final String LAST_SUCCESS_METRIC = "hub_vault_unlock_last_success_epoch_seconds";
	private static final String LAST_FAILURE_METRIC = "hub_vault_unlock_last_failure_epoch_seconds";

	@Inject
	Meter meter;

	private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);
	private final AtomicLong lastFailureEpochSeconds = new AtomicLong(0);
	private LongCounter unlockCounter;

	@PostConstruct
	void registerInstruments() {
		unlockCounter = meter.counterBuilder(UNLOCKS_TOTAL_METRIC)
				.setDescription("Total number of vault unlock attempts")
				.build();
		meter.gaugeBuilder(LAST_SUCCESS_METRIC)
				.ofLongs()
				.setDescription("Epoch timestamp in seconds of the last successful vault unlock")
				.buildWithCallback(measurement -> measurement.record(lastSuccessEpochSeconds.get()));
		meter.gaugeBuilder(LAST_FAILURE_METRIC)
				.ofLongs()
				.setDescription("Epoch timestamp in seconds of the last failed vault unlock")
				.buildWithCallback(measurement -> measurement.record(lastFailureEpochSeconds.get()));
	}

	public void recordUnlock() {
		unlockCounter.add(1);
	}

	public void recordSuccess() {
		lastSuccessEpochSeconds.set(currentEpochSeconds());
	}

	public void recordFailure() {
		lastFailureEpochSeconds.set(currentEpochSeconds());
	}

	private long currentEpochSeconds() {
		return System.currentTimeMillis() / 1000;
	}
}
