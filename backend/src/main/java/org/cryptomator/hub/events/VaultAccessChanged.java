package org.cryptomator.hub.events;

/**
 * CDI event fired (from within a transaction) when something has been written that may have altered the set of users
 * who can access vaults but have no per-user access token yet. Observers should react after the firing transaction
 * commits — see {@link VaultAccessChangeBroadcaster}.
 *
 * <p>The event carries no payload: every observer that cares re-queries the database. This keeps fire sites trivial
 * (one line per write path) and avoids the bookkeeping of tracking which vault was affected when group/membership
 * changes ripple transitively.
 */
public record VaultAccessChanged() {
}
