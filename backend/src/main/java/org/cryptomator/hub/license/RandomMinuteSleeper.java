package org.cryptomator.hub.license;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Random;

@ApplicationScoped
public class RandomMinuteSleeper {

	private static final long MINUTE_IN_MILLIS = 60 * 1000L;
	private static final Random RNG = new Random();

	void sleep() throws InterruptedException {
		Thread.sleep(RNG.nextInt(0, 60) * MINUTE_IN_MILLIS);
	}

}
