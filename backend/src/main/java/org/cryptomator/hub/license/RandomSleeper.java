package org.cryptomator.hub.license;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.random.RandomGenerator;

@ApplicationScoped
public class RandomSleeper {

	private final RandomGenerator rng;

	public RandomSleeper() {
		this.rng = new SecureRandom(); // RandomGenerator.getDefault() is seeded at build time in native images, making all Hub deployments sleep equally long
	}

	void sleep(int min, int max, TemporalUnit unit) throws InterruptedException {
		var delay = Duration.of(rng.nextInt(min, max), unit);
		Thread.sleep(delay.toMillis());
	}

}
