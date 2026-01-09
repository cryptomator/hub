package org.cryptomator.hub.entities;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Batch {

	private final int size;

	private Batch(int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("Batch size must be positive");
		}
		this.size = size;
	}

	public static Batch of(int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("Batch size must be positive");
		}
		return new Batch(size);
	}

	// TODO: add jspecify annotations
	public <T> void run(Collection<T> collection, Consumer<List<T>> job) {
		run(collection, null, (batch, ignored) -> {
			job.accept(batch);
			return null;
		});
	}

	// TODO: add jspecify annotations
	public <T, R> R run(Collection<T> collection, R initialValue, BiFunction<List<T>, R, R> job) {
		if (collection == null || collection.isEmpty()) {
			return initialValue;
		}
		List<T> list = collection instanceof List<T> l ? l : List.copyOf(collection);
		R result = initialValue;
		for(int i = 0; i < list.size(); i += size) {
			List<T> sublist = list.subList(i, Math.min(i + size, list.size()));
			result = job.apply(sublist, result);
		}
		return result;
	}

}
