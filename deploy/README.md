# Deploying Cryptomator Hub

Hub consists of three services: the Hub application itself, [Keycloak](https://www.keycloak.org/) as identity provider and a PostgreSQL database shared by both. Pick the deployment scenario that fits you:

| I want to… | Go to |
|---|---|
| Try Hub on my machine with a single `docker compose up` | [`compose/local/`](compose/local/README.md) |
| Run Hub in production on Kubernetes | [`../chart/`](../chart/README.md) (Helm chart) |
| Develop Hub and run a locally built image against the dev realm | [`compose/dev/`](compose/dev/README.md) |
