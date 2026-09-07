# Local testing with Docker Compose

A self-contained Cryptomator Hub stack with pinned image versions for trying Hub on your machine. [`compose.yaml`](compose.yaml) is the only file you need; it does not reference anything else in this repository.

Requirements: Docker Compose ≥ 2.23.1.

1. Download [`compose.yaml`](compose.yaml) into an empty directory.
2. `docker compose up`
3. Wait until all services are `healthy` (Keycloak imports the realm on first start), then open Hub at <http://localhost:8080> or the Keycloak admin console at <http://localhost:8180>, both `admin` / `admin`.

Everything runs on plain HTTP with well-known passwords — for evaluation only. To change ports or hostnames, follow the comments in `compose.yaml`; the realm is imported on Keycloak's first boot only, so `docker compose down -v` to start over after changing URLs.

For real deployments see [`../prod/`](../prod/README.md) or [`../../helm/prod/`](../../helm/prod/README.md); for everything else — license, user management, connecting Cryptomator apps — see <https://docs.cryptomator.org/hub/>.
