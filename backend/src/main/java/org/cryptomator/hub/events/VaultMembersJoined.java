package org.cryptomator.hub.events;

/**
 * CDI event fired (from within a transaction) when one or more users have <em>joined</em> a vault — directly or
 * transitively via group membership — and may therefore be awaiting a per-user access token. It is deliberately a
 * one-directional "grant work may now exist" signal, not a symmetric change feed: removals and role-only updates never
 * create members lacking a token, so they do not fire it. Observers should react after the firing transaction commits —
 * see {@link VaultMembersJoinedBroadcaster}.
 *
 * <p>The event carries no payload: every observer that cares re-queries the database. This keeps fire sites trivial
 * (one line per write path) and avoids the bookkeeping of tracking which vault was affected when group/membership
 * additions ripple transitively.
 */
public record VaultMembersJoined() {
}
