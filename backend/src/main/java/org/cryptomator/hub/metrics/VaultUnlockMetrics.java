package org.cryptomator.hub.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
	MeterRegistry meterRegistry;

	private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);
	private final AtomicLong lastFailureEpochSeconds = new AtomicLong(0);
	private Counter unlockCounter;

	@PostConstruct
	void registerGauges() {
		unlockCounter = Counter.builder(UNLOCKS_TOTAL_METRIC)
				.description("Total number of vault unlock attempts")
				.register(meterRegistry);
		Gauge.builder(LAST_SUCCESS_METRIC, lastSuccessEpochSeconds, AtomicLong::get)
				.description("Epoch timestamp in seconds of the last successful vault unlock")
				.register(meterRegistry);
		Gauge.builder(LAST_FAILURE_METRIC, lastFailureEpochSeconds, AtomicLong::get)
				.description("Epoch timestamp in seconds of the last failed vault unlock")
				.register(meterRegistry);
	}

	public void recordUnlock() {
		unlockCounter.increment();
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
