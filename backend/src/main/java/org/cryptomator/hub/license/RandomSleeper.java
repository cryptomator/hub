package org.cryptomator.hub.license;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.random.RandomGenerator;

@ApplicationScoped
public class RandomSleeper {

	private final RandomGenerator rng;

	public RandomSleeper() {
		this.rng = RandomGenerator.getDefault();
	}

	void sleep(int min, int max, TemporalUnit unit) throws InterruptedException {
		var delay = Duration.of(rng.nextInt(min, max), unit);
		Thread.sleep(delay.toMillis());
	}

}
