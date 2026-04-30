# Cryptomator Hub Helm Chart

This chart deploys:

- Cryptomator Hub (required)
- Keycloak (optional, enabled by default)
- PostgreSQL (optional, enabled by default)

Image repositories/tags are fixed in templates:
- Hub: `ghcr.io/cryptomator/hub:<appVersion from Chart.yaml>`
- Keycloak: `ghcr.io/cryptomator/keycloak:26.5.3`
- PostgreSQL: `postgres:17-alpine`

TLS termination is currently expected to be done by ingress controller.
Supported ingress controller templates:
- `ingress.controller=nginx`
- `ingress.controller=traefik`
- `ingress.controller=contour`


## Quick Start (Full Internal Stack)

Assuming you have a local KIND cluster, e.g. via [Podman Desktop](https://podman-desktop.io/) with contour ingress on port 9090:

```bash
helm install hub charts/cryptomator-hub \
  --namespace cryptomator \
  --create-namespace \
  --wait --timeout 5m \
  --set urls.hub.public=http://localhost:9090/hub \
  --set urls.kc.public=http://localhost:9090/kc \
  --set ingress.controller=nginx \
  --set hub.admin.password=password
```

Passwords are optional by default. If unset, the chart generates random values and
prints commands in `helm` notes to retrieve them from Kubernetes Secrets.

The Keycloak realm import is rendered from a dedicated template using:

- `keycloak.realmBootstrap.realmId`
- `hub.secrets.systemClientSecret` (optional; auto-generated when chart-managed Hub secret is used)
- `hub.admin.*` (realm-level Hub admin user; separate from `keycloak.admin.*` bootstrap user)

## Telemetry (OpenTelemetry)

Hub exports metrics, traces and logs via OpenTelemetry / OTLP. Telemetry is **off by default**; enable it via:

- `hub.metrics.enabled` (default `false`)
- `hub.metrics.endpoint` — OTLP/gRPC endpoint, default `http://otel-collector:4317`
- `hub.metrics.resourceAttributes` — extra OTel resource attributes merged into the chart defaults (`service.name`, `service.version`). Setting a key with the same name overrides the default.
- `hub.metrics.otlp.username` / `hub.metrics.otlp.password` — Credentials used to add `QUARKUS_OTEL_EXPORTER_OTLP_HEADERS` header `Authorization: Basic <base64(user:pass)>`.

When disabled, the chart sets `QUARKUS_OTEL_SDK_DISABLED=true` so the SDK does not start. When enabled, the chart sets `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT` and Hub pushes to your collector — there is no `/q/metrics` scrape endpoint anymore. To bridge to Prometheus, run an OpenTelemetry Collector with `prometheus` or `prometheusremotewrite` exporter.

## Hub with External PostgreSQL and Keycloak

```bash
helm install hub charts/cryptomator-hub \
  --namespace cryptomator \
  --create-namespace \
  --wait --timeout 5m \
  --set keycloak.enabled=false \
  --set postgres.enabled=false \
  --set hub.database.jdbcUrl='jdbc:postgresql://db.example:5432/hub' \
  --set hub.database.username='hub' \
  --set hub.config.keycloakPublicUrl='https://sso.example/kc' \
  --set hub.config.keycloakLocalUrl='http://keycloak.svc.cluster.local:8080/kc' \
  --set hub.oidc.authServerUrl='http://keycloak.svc.cluster.local:8080/kc/realms/cryptomator' \
  --set hub.oidc.tokenIssuer='https://sso.example/kc/realms/cryptomator'
```

### Importing `realm.json`

Even with `keycloak.enabled=false`, the chart still renders `realm.json` in Secret `<release>-keycloak` so you can manually export/import it for your existing Keycloak.

Assuming namespace `cryptomator` and name `hub`:

```bash
kubectl get secret -n cryptomator hub-secrets-kc -o jsonpath='{.data.realm\.json}' | base64 -d | ...
```

## Verify Published Chart (Signature + Provenance)

This chart contains a OCI chart signature, which can be verified as follows (assuming chart version `0.1.0`):

```bash
cosign verify \
  --certificate-identity-regexp 'https://github.com/cryptomator/hub/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/cryptomator/charts/cryptomator-hub:0.1.1
```

You can additionally inspect provenance attestations:

```bash
cosign verify-attestation \
  --type https://slsa.dev/provenance/v1 \
  --certificate-identity-regexp 'https://github.com/cryptomator/hub/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/cryptomator/charts/cryptomator-hub:0.1.1
```
