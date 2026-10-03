package org.cryptomator.hub.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * In-process pub/sub for {@link VaultMembersJoined} events. Long-polling request handlers acquire a {@link Ticket},
 * check the current database state, and then block on the ticket until the next event fires or the timeout elapses.
 *
 * <p>The observer is wired with {@link TransactionPhase#AFTER_SUCCESS} so a rolled-back transaction does not produce
 * spurious wake-ups. Best-effort by design: events are not persisted and listeners that join after a fire miss it —
 * a periodic poll cadence on the client is the intended backstop.
 */
@ApplicationScoped
public class VaultMembersJoinedBroadcaster {

	private final Set<CompletableFuture<Void>> waiters = ConcurrentHashMap.newKeySet();

	void onMembersJoined(@Observes(during = TransactionPhase.AFTER_SUCCESS) VaultMembersJoined event) {
		for (var w : waiters) {
			w.complete(null);
		}
	}

	/**
	 * @return a {@link Ticket} that wakes on the next {@link VaultMembersJoined} fired after this call. Must be closed.
	 */
	public Ticket subscribe() {
		var future = new CompletableFuture<Void>();
		waiters.add(future);
		return new Ticket(future, () -> waiters.remove(future));
	}

	public record Ticket(CompletableFuture<Void> future, Runnable cleanup) implements AutoCloseable {

		/**
		 * Blocks the calling (virtual) thread until either an event fires or the timeout elapses. Swallows
		 * timeout and execution exceptions — the contract is "best effort, return when there's news or after a while".
		 */
		public void awaitChange(Duration timeout) throws InterruptedException {
			try {
				future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			} catch (TimeoutException | ExecutionException ignored) {
				// timeout: no event arrived in time — caller re-queries either way.
				// execution: future completed exceptionally — same behavior.
			}
		}

		@Override
		public void close() {
			cleanup.run();
		}
	}
}
