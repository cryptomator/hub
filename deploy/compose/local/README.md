# Local testing with Docker Compose

A self-contained Cryptomator Hub stack with pinned image versions for trying Hub on your machine. [`compose.yaml`](compose.yaml) is the only file you need; it does not reference anything else in this repository.

Requirements: Docker Compose ≥ 2.23.1 (inline `configs.content`).

## Quick start

Download [`compose.yaml`](compose.yaml) (or copy it from this directory) and start the stack:

```bash
docker compose up
```

Wait until all three services report `healthy` (about a minute on first start, Keycloak imports the realm), then open:

| URL | Credentials |
|---|---|
| Hub: <http://localhost:8080> | `admin` / `admin` |
| Keycloak admin console: <http://localhost:8180> | `admin` / `admin` |

On first login Hub asks for a license. Any Hub license works for testing; see <https://cryptomator.org/hub/> on how to obtain one. Afterwards, add users and groups in Keycloak (realm `cryptomator`) — Hub syncs them every minute. To connect a Cryptomator desktop or mobile app, create a vault in Hub and follow the instructions shown there.

## What the stack contains

- `postgres:17.11-alpine` with two databases (`hub`, `keycloak`), persisted in the named volume `postgres-data`.
- `ghcr.io/cryptomator/keycloak` — the stock Keycloak image plus the Cryptomator login theme and `curl` for the health check. A minimal `cryptomator` realm (one admin user, the OIDC clients `cryptomatorhub`, `cryptomator` and `cryptomatorhub-system`) is embedded in the compose file and imported on first boot. It matches what the Helm chart renders in `chart/templates/_realm.tpl`.
- `ghcr.io/cryptomator/hub` — the Hub application, configured through environment variables; the same settings the Helm chart uses.

## Changing ports or hostnames

The two published ports (`8080` for Hub, `8180` for Keycloak) appear in several places that must stay consistent: the `ports` mappings, `KC_HOSTNAME`, `HUB_KEYCLOAK_PUBLIC_URL`, `QUARKUS_OIDC_TOKEN_ISSUER`, the `connect-src` entry of the Content Security Policy, and the `redirectUris` / `frame-ancestors` of the embedded realm. Update all of them together.

> [!TIP]
> Keycloak imports the realm **only on first boot** — `--import-realm` never overwrites an existing realm. If you change URLs after the first start, Keycloak keeps the old redirect URIs and login fails with `Invalid parameter: redirect_uri`. Either edit the `cryptomatorhub` client in the Keycloak admin console, or reset everything with `docker compose down -v` (this deletes the database, including all vaults).

## Not for production

This file is meant for evaluation only:

- All passwords (`admin`, `hub`, `keycloak`, `top-secret`) are well-known defaults.
- Everything runs on plain HTTP on `localhost`; no TLS is configured. Hub's crypto happens in the browser and requires a secure context, which browsers grant to `localhost` but not to other plain-HTTP hosts.
- No backups, resource limits or upgrade strategy.

For real deployments use the Helm chart, see [`../../helm/prod/`](../../helm/prod/README.md) — it generates secrets, supports external PostgreSQL/Keycloak and terminates TLS at the ingress.

## Upgrading

Image versions are pinned in `compose.yaml`. To upgrade, change the tags and run `docker compose up -d`. Hub applies database migrations automatically at start; Keycloak upgrades follow the [Keycloak upgrade guide](https://www.keycloak.org/docs/latest/upgrading/). Always back up the `postgres-data` volume first.
