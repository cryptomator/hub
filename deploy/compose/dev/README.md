# Development stack with Docker Compose

Runs a **locally built** Hub image together with Keycloak and PostgreSQL, wired up exactly like the Quarkus dev services that `./mvnw quarkus:dev` starts. Use it to test a container build of Hub (e.g. the native image) end-to-end before pushing, or to poke at memory settings of the native binary.

If you just want to try Hub, use [`../local/`](../local/README.md) instead — it needs nothing from this repository.

## Prerequisites

- A checkout of this repository: [`compose.yaml`](compose.yaml) bind-mounts the dev realm from `backend/src/main/resources/cryptomator-realm.json`.
- A locally built Hub image tagged `ghcr.io/cryptomator/hub:native`. From the repository root (frontend must be built into the backend first, see [`backend/README.md`](../../../backend/README.md)):

  ```bash
  docker build -f backend/src/main/docker/Dockerfile.native -t ghcr.io/cryptomator/hub:native backend
  ```

## Usage

```bash
docker compose up
```

Paths in `compose.yaml` are resolved relative to this directory, so the command works from anywhere via `docker compose -f deploy/compose/dev/compose.yaml up`.

| URL | Credentials |
|---|---|
| Hub: <http://localhost:8080> | `admin` / `admin`, `alice` / `asd`, `bob` / `asd`, … (see the dev realm) |
| Keycloak admin console: <http://localhost:8180> | `admin` / `admin` |

The dev realm is imported on Keycloak's first boot only; run `docker compose down -v` to reset the database and re-import it.

## Differences to the local stack

- Hub runs from the `:native` tag you built, with a 48 MiB memory limit and `-XX:MaximumHeapSizePercent` / `MALLOC_ARENA_MAX` overrides for measuring the native image's footprint. Adjust or drop these when you're not interested in memory behaviour.
- The realm comes from `backend/src/main/resources/cryptomator-realm.json` and contains the well-known dev users, the `cryptomatorhub-cli` client and `localhost:3000` (Vite dev server) as an allowed redirect URI.
