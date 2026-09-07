package org.cryptomator.hub.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class BatchTest {

	@ParameterizedTest
	@ValueSource(ints = {-1, 0})
	void testInvalidBatchSize(int batchSize) {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Batch.of(batchSize);
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 5, 10})
	void testBatchSizes(int batchSize) {
		List<Integer> result = new ArrayList<>();
		Batch.of(batchSize).run(List.of(1, 2, 3, 4, 5, 6, 7), result::addAll);

		Assertions.assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), result);
	}

	@Test
	void testNoopIfInputIsEmpty() {
		Consumer<List<String>> job = Mockito.mock();

		Batch.of(10).run(List.of(), job);

		Mockito.verifyNoInteractions(job);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 5, 10})
	void testBatchSizesReduce(int batchSize) {
		int result = Batch.of(batchSize).run(List.of(1, 2, 3, 4, 5, 6, 7), 0, (sublist, r) -> {
			for (int i : sublist) {
				r += i;
			}
			return r;
		});

		Assertions.assertEquals(1 + 2 + 3 + 4 + 5 + 6 + 7, result);
	}

	@Test
	void testNoopIfReduceInputIsEmpty() {
		BiFunction<List<Integer>, Integer, Integer> job = Mockito.mock();

		Batch.of(10).run(List.of(), 0, job);

		Mockito.verifyNoInteractions(job);
	}

}