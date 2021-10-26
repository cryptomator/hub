package org.cryptomator.hub.config;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Placeholder class! TODO replace as soon as we support dynamic config loading
 */
@Deprecated
@ApplicationScoped
public class Config {

	private boolean setup;

	public boolean isSetup() {
		return setup;
	}

	public void setSetup(boolean setup) {
		this.setup = setup;
	}
}
