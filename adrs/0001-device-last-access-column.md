# 1. Store device "last access" as a column instead of deriving it from audit events

Date: 2026-06-16

## Status

Accepted

Supersedes the approach proposed in [PR #435](https://github.com/cryptomator/hub/pull/435).

## Context

The admin user-details view (and the user's own device list) shows, per device, the
*last vault access*: the timestamp and IP address of the most recent **successful** vault-key
retrieval — i.e. an actual unlock by that device, not an arbitrary login or a denied attempt.

This data has so far been reconstructed on read from the audit-event log. The
`audit_event_vault_key_retrieve` table receives one row on **every** vault unlock
(`EventLogger.logVaultKeyRetrieved`, called from both `VaultResource.unlock` and the legacy
`legacyUnlock`). [PR #435](https://github.com/cryptomator/hub/pull/435) extended this by
fetching last access for admins via a `LEFT JOIN` against that table with a correlated
`MAX(id)`-per-device subquery.

Two problems were raised with deriving last access from audit events:

1. **The audit table grows without bound.** It gains a row per unlock and is never purged —
   the license setting `auditLogRetentionDays` only gates *querying* the log in
   `AuditLogResource`, it does not delete rows. A per-device `MAX` subquery over an
   ever-growing table degrades over time.
2. **It couples a basic feature to a licensed subsystem.** "When was this device last used"
   is basic device management, yet audit-log access is license-gated. Reading last access
   straight from the event table also sidesteps the retention gate inconsistently.

"Last access" is conceptually a property of the device. Modelling it as derived event-stream
state is both slower and less honest than storing it on the device.

## Decision

Store last access directly on the `device` table.

- Add two columns to `device`: `last_access_time TIMESTAMP WITH TIME ZONE` and
  `last_ip_address VARCHAR(46)` (including migration `V26__Device_Last_Access.sql`).
- On a modern unlock that succeeds (`VaultResource.unlock`, result `SUCCESS` only), write
  both columns for the accessing device via `Device.Repository.updateLastAccess(...)`,
  in the same transaction as the audit event.
- Read last access straight off the entity in `DeviceResource.DeviceDto.fromEntity(Device)`;
  the admin `getUser` and the `getMe` endpoints no longer issue a separate audit query for
  real devices.
- Continue to record the `VaultKeyRetrievedEvent` audit event unchanged. The column is a
  denormalised cache of it; the audit log remains the immutable history.
- Legacy unlock/legacy devices are still updated by querying the audit table due to planned removal
  [#333](https://github.com/cryptomator/hub/issues/333).

## Consequences

### Positive

- Reading last access is an O(1) column read, independent of audit-table size.
- The feature no longer depends on the audit-log entitlement or its retention window.
- Simpler read paths: Per-device audit query for real devices are removed.
- The frontend is unchanged — `DeviceDto` already exposes `lastAccessTime`/`lastIpAddress`.

### Negative / trade-offs

- Latest IP now persists indefinitely on the device row, outliving audit-log
  retention. This is a deliberate privacy trade-off accepted for parity with the existing UI.
- Each unlock performs one extra `UPDATE device` next to the event insert. Unlocks per device
  are infrequent enough (Assumption: on average at most 1 unlock per 15min), so the added write / row-contention cost is negligible.
- The column is a denormalised duplicate of audit data and must be kept in step. This is
  contained to the single unlock chokepoint.
- Two mechanisms coexist until #333: columns for real devices, audit-derived for legacy.

## Alternatives considered

- **Audit-derived read (PR #435).** Rejected: degrades with the growing audit table and
  couples the feature to the licensed audit subsystem.
- **Timestamp-only column, IP kept audit-only.** More privacy-conscious, but would drop the
  IP from the device list (a UI regression) and was not chosen.
