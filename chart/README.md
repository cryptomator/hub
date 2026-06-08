# Cryptomator Hub Helm Chart

This chart deploys:

- Cryptomator Hub (required)
- Keycloak (optional, enabled by default)
- PostgreSQL (optional, enabled by default)

Image repositories are fixed in templates; tags are overridable per workload:
- Hub: `ghcr.io/cryptomator/hub:<hub.image.tag>` (defaults to chart `appVersion`)
- Keycloak: `ghcr.io/cryptomator/keycloak:<keycloak.image.tag>` (default `26.6.2`)
- PostgreSQL: `postgres:<postgres.image.tag>` (default `17-alpine`)

TLS termination is currently expected to be done by ingress controller.
Supported ingress controller templates:
- `ingress.controller=nginx`
- `ingress.controller=traefik`


## Quick Start (Full Internal Stack)

Assuming you have a local Minikube cluster, e.g. via [Podman Desktop](https://podman-desktop.io/) with nginx ingress on port 9090:

```bash
# one-off (skip if your cluster already has nginx-ingress)
minikube addons enable ingress

# deploy
helm install hub chart \
  --namespace cryptomator \
  --create-namespace \
  --wait --timeout 5m \
  --set urls.hub.public=http://localhost:9090/hub \
  --set urls.kc.public=http://localhost:9090/kc \
  --set ingress.controller=nginx \
  --set hub.admin.password=password
```

In a separate terminal, expose the ingress controller on `localhost:9090`:

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 9090:80
```

Once both commands are running:

| URL | Credentials |
|---|---|
| Hub UI: <http://localhost:9090/hub> | `admin` / `admin` |
| Keycloak admin: <http://localhost:9090/kc> | `admin` / `admin` |

> [!WARNING]
> `values-demo.yaml` is for local evaluation only: it uses well-known passwords, retains secrets on `helm uninstall`, and requests minimal storage. Start from `values-prod.yaml` for real deployments.

<!-- separates the two GitHub alerts above/below (avoids markdownlint MD028) -->

> [!TIP]
> Keycloak imports the realm **only on first boot** (`--import-realm` does not overwrite an existing realm). Because Postgres data persists across reinstalls — and the demo additionally keeps Secrets via `keepOnUninstall` — changing `urls.hub.public` / `urls.kc.public` after the first install leaves the realm (and its OIDC redirect URIs) stale, which surfaces as a Keycloak `Invalid parameter: redirect_uri` error. To pick up new URLs, either edit the client in the Keycloak admin console, or reset the realm:
>
> ```bash
> helm uninstall hub -n cryptomator
> kubectl delete pvc -n cryptomator data-hub-pg-0
> kubectl delete secret -n cryptomator hub-secrets-hub hub-secrets-kc hub-secrets-pg --ignore-not-found
> # then reinstall
> ```

### Routing: subdomains vs. subpaths

The ingress layout is derived entirely from the public URLs you provide — the chart parses host and path independently, so both styles work with the same templates:

- **Subdomains** (as in the demo): give each component a bare host, e.g. `urls.hub.public=https://hub.example.com`, `urls.kc.public=https://kc.example.com`. Each is served at `/`. Keycloak then lives on a different origin than Hub; the chart automatically allowlists it in Hub's `Content-Security-Policy` (`connect-src`), so the browser can reach the OIDC endpoints.
- **Subpaths**: share one host with distinct prefixes, e.g. `urls.hub.public=https://example.com/hub`, `urls.kc.public=https://example.com/kc`. The chart adds the controller-specific path rewrite (nginx regex / Traefik `stripPrefix`) automatically.

Passwords are optional by default. If unset, the chart generates random values and
prints commands in `helm` notes to retrieve them from Kubernetes Secrets.

The Keycloak realm import is rendered from a dedicated template using:

- `keycloak.realmBootstrap.realmId`
- `hub.secrets.systemClientSecret` (optional; auto-generated when chart-managed Hub secret is used)
- `hub.admin.*` (realm-level Hub admin user; separate from `keycloak.admin.*` bootstrap user)

## Telemetry (OpenTelemetry)

Hub exports metrics, traces and logs via OpenTelemetry / OTLP. Telemetry is **off by default**; enable it via:

- `hub.metrics.enabled` (default `false`)
- `hub.metrics.endpoint` — OTLP endpoint, default `https://otel-collector:443`
- `hub.metrics.protocol` — OTLP wire protocol: `http/protobuf` (default) or `grpc`
- `hub.metrics.resourceAttributes` — extra OTel resource attributes merged into the chart defaults (`service.name`, `service.version`). Setting a key with the same name overrides the default.
- `hub.metrics.otlp.username` / `hub.metrics.otlp.password` — Credentials used to add `QUARKUS_OTEL_EXPORTER_OTLP_HEADERS` header `Authorization: Basic <base64(user:pass)>`.

When disabled, the chart sets `QUARKUS_OTEL_SDK_DISABLED=true` so the SDK does not start. When enabled, the chart sets `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT` and Hub pushes to your collector — there is no `/q/metrics` scrape endpoint anymore. To bridge to Prometheus, run an OpenTelemetry Collector with `prometheus` or `prometheusremotewrite` exporter.

## Hub with External PostgreSQL and Keycloak

```bash
helm install hub chart \
  --namespace cryptomator \
  --create-namespace \
  --wait --timeout 5m \
  --set keycloak.enabled=false \
  --set postgres.enabled=false \
  --set hub.database.jdbcUrl='jdbc:postgresql://db.example:5432/hub' \
  --set hub.database.username='hub' \
  --set urls.kc.public='https://sso.example/kc' \
  --set urls.kc.clusterInternal='http://keycloak.svc.cluster.local:8080/kc' \
  --set urls.kc.authServerUrl='http://keycloak.svc.cluster.local:8080/kc/realms/cryptomator' \
  --set urls.kc.tokenIssuer='https://sso.example/kc/realms/cryptomator'
```

### Importing `realm.json`

Even with `keycloak.enabled=false`, the chart still renders `realm.json` in Secret `<release>-secrets-kc` so you can manually export/import it for your existing Keycloak.

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
