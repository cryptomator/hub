package org.cryptomator.hub.license;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Random;

@ApplicationScoped
public class RandomMinuteSleeper {

	private static final long MINUTE_IN_MILLIS = 60 * 1000L;
	private final Random rng;

	public RandomMinuteSleeper() {
		this.rng = new Random();
	}

	void sleep() throws InterruptedException {
		Thread.sleep(rng.nextInt(0, 60) * MINUTE_IN_MILLIS);
	}

}
