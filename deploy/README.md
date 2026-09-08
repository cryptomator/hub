# Cryptomator Hub Deployment Examples

Hub consists of three services: the Hub application itself, [Keycloak](https://www.keycloak.org/) as identity provider and a PostgreSQL database shared by both. The guides below are **examples** for common scenarios — each one is a complete, tested walkthrough, but by no means the only way to run Hub. Every option they use is a regular chart value or compose setting, so mix and match as your environment requires (see [`values.yaml`](../chart/values.yaml) for everything the chart can do). Pick the scenario closest to yours:

| I want to… | Go to |
|---|---|
| Try Hub on my machine with a single `docker compose up` | [`compose/local/`](compose/local/README.md) |
| Run Hub in production on a single Docker host | [`compose/prod/`](compose/prod/README.md) |
| Run Hub in production on Kubernetes | [`helm/prod/`](helm/prod/README.md) |
| Connect Hub to a Keycloak I already operate | [`helm/existing-keycloak/`](helm/existing-keycloak/README.md) |
| Develop Hub and run a locally built image against the dev realm | [`compose/dev/`](compose/dev/README.md) |
| Develop the Helm chart or test a locally built image on Kubernetes | [`helm/dev/`](helm/dev/README.md) |
