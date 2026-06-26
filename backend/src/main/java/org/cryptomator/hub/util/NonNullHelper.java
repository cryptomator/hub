package org.cryptomator.hub.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Gatherer;

public class NonNullHelper {

	private NonNullHelper() {}

	public static <T extends @Nullable Object> @NonNull T enforce(@Nullable T value) {
		return Objects.requireNonNull(value);
	}

	public static <T extends @Nullable Object> Gatherer.Integrator<Object, @Nullable T, @NonNull T> gather() {
		return (_, element, downstream) -> {
			if (element != null) {
				downstream.push(element);
			}
			return true;
		};
	}

}
