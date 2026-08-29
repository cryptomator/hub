# Development stack with Docker Compose

Runs a **locally built** Hub image together with Keycloak and PostgreSQL, wired up like the Quarkus dev services that `./mvnw quarkus:dev` starts. Use it to test a container build of Hub end-to-end, or to poke at memory settings of the native binary. If you just want to try Hub, use [`../local/`](../local/README.md) instead.

Prerequisites:

- A checkout of this repository; all commands below run from its root. [`compose.yaml`](compose.yaml) bind-mounts the dev realm from `backend/src/main/resources/cryptomator-realm.json`.
- Docker Compose ≥ 2.23.1, `pnpm`.

1. Build the frontend, then the backend as a native image (the Dockerfile only copies `backend/src`, so the frontend must be built into it first):

   ```bash
   (cd frontend && pnpm install && pnpm dist)
   docker build -f backend/src/main/docker/Dockerfile.native -t ghcr.io/cryptomator/hub:native backend
   ```
2. `docker compose -f deploy/compose/dev/compose.yaml up`
3. Wait until all services are `healthy`, then open Hub at <http://localhost:8080> (`admin` / `admin`, `alice` / `asd`, `bob` / `asd`, … from the dev realm) or the Keycloak admin console at <http://localhost:8180> (`admin` / `admin`).

The `hub` service runs with a 48 MiB memory limit and `-XX:MaximumHeapSizePercent` / `MALLOC_ARENA_MAX` overrides for measuring the native image's footprint; adjust or drop them in `compose.yaml` when you're not interested in memory behaviour. The realm is imported on Keycloak's first boot only — `docker compose -f deploy/compose/dev/compose.yaml down -v` to start over.
