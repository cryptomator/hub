package org.cryptomator.hub.entities;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Batch {

	private final int size;

	private Batch(int size) {
		this.size = size;
	}

	public static Batch of(int size) {
		return new Batch(size);
	}

	public <T> void run(Collection<T> collection, Consumer<List<T>> job) {
		List<T> list = collection instanceof List<T> l ? l : List.copyOf(collection);
		for(int i = 0; i < list.size(); i += size) {
			List<T> sublist = list.subList(i, Math.min(i + size, list.size()));
			job.accept(sublist);
		}
	}

	public <T, R> R run(Collection<T> collection, R initialValue, BiFunction<List<T>, R, R> job) {
		List<T> list = collection instanceof List<T> l ? l : List.copyOf(collection);
		R result = initialValue;
		for(int i = 0; i < list.size(); i += size) {
			List<T> sublist = list.subList(i, Math.min(i + size, list.size()));
			result = job.apply(sublist, result);
		}
		return result;
	}

	public <T, R> List<R> run(Collection<T> collection, BiFunction<List<T>, List<R>, List<R>> job) {
		List<T> list = collection instanceof List<T> l ? l : List.copyOf(collection);
		List<R> result = List.of();
		for(int i = 0; i < list.size(); i += size) {
			List<T> sublist = list.subList(i, Math.min(i + size, list.size()));
			result = job.apply(sublist, result);
		}
		return result;
	}
}
