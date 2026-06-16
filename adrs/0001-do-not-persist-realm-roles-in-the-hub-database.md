# 1. Do not persist realm roles in the Hub database

Date: 2026-06-15

## Status

Accepted

## Context

Cryptomator Hub authenticates and authorizes API consumers via Keycloak-issued
JWTs. Realm roles (`user`, `admin`, `create-vaults`) are claims in the access
token, and authorization (`@RolesAllowed`) is enforced entirely from the token.
Keycloak is therefore the authoritative store for realm roles.

Historically, the Hub *also* mirrored each user's realm roles into a
`user_details.realm_roles` column (added in migration `V23`). This copy was kept
in sync from two places:

- `KeycloakAdminService.syncUser` / `updateUserRoles` on the write paths, and
- `KeycloakAuthorityPuller`, the scheduled syncer, which additionally queried
  `KeycloakAuthorityProvider.usersInRole(...)` because `UserRepresentation`
  does not reliably carry roles in list responses.

The only consumer of the persisted copy was *display*: the `realmRoles` field on
`UserDto`. The data was never used for authorization.

This mirroring was the root cause of a class of drift bugs (the column
disagreeing with Keycloak) and motivated a fix branch dedicated to "syncing
realm roles". During review we questioned whether the copy should exist at all.

Findings that informed the decision:

- The current user's roles are already in their JWT, so the self-view (`GET
  /me`) never needed the DB copy.
- No list/bulk view displays other users' roles. The frontend user list does
  not render roles; only the single-user **detail** and **edit** screens do, and
  both are fed by one endpoint: `GET /users/{id}`.
- Keycloak's admin API exposes role mappings per user
  (`/users/{id}/role-mappings/realm`); there is no efficient bulk read. A
  single-user, on-demand read is cheap (one call when an admin opens a user).

## Decision

Stop persisting realm roles in the Hub database. Treat Keycloak as the single
source of truth and serve roles for display on demand.

Concretely:

1. Drop the `user_details.realm_roles` column (migration `V26`) and remove the
   field from the `User` entity.
2. Remove all role mirroring: from `syncUser`, from the `KeycloakAuthorityPuller`,
   and the now-dead `KeycloakAuthorityProvider.usersInRole(...)`.
3. `KeycloakAdminService.updateUserRoles` writes only to Keycloak (no DB write,
   not transactional); an unknown user surfaces as Keycloak's `404`.
4. Add `KeycloakAdminService.realmRolesOf(userId)` to read a single user's realm
   roles from Keycloak's role-mapping API. Its result is cached for 30s to prevent
   spamming requests.
5. Roles are removed from the base `UserDto` entirely and instead live on the
   detailed view `UserDto.WithDetails`, since only `GET /users/{id}` (admin-only)
   serves them. That endpoint always reads them through from Keycloak, so
   `realmRoles` is a non-nullable, always-present field of `WithDetails`. This
   keeps the widely-shared base DTO free of a role field and makes "the detailed
   view carries roles" a structural guarantee.
6. Role *input* is unchanged: `POST /users` and `PUT /users/{id}` still accept
   `realmRoles` and write them to Keycloak. It invalidates the cache for
   `KeycloakAdminService.realmRolesOf(userId)`.

## Consequences

### Positive

- Eliminates realm-role drift entirely — the bug class is removed by deletion,
  not patched.
- Less code and no scheduled role reconciliation: the syncer, the shared
  read-back helper, and the `usersInRole` reader are gone.
- A single, clear source of truth (Keycloak) for both authorization and display.

### Negative / trade-offs

- Viewing a single user as an admin now costs one extra Keycloak admin call.
  Acceptable because it only happens on the detail/edit screens, one user at a
  time.
- Roles are confined to the detailed view: only `GET /users/{id}` carries them,
  and every fetch of `WithDetails` pays the Keycloak call.
  This was a deliberate decision against a query parameter for a simpler
  contract since admin is the sole consumer.
- If a future feature needs roles for *many* users at once (e.g. a roles column
  in the user list), this decision should be revisited: a per-user read-through
  would become an N+1 against the Keycloak admin API, and either a bulk fetch or
  a (carefully reconciled) cache would be needed.

### Neutral

- Keycloak remains configured with the realm roles in `cryptomator-realm.json`;
  that is unaffected.
