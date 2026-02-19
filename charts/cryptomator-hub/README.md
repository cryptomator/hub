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


## Quick Start (Full Internal Stack)

```bash
helm install hub charts/cryptomator-hub \
  --namespace default \
  --set ingress.enabled=true \
  --set global.host=domain.tld \
  --set postgres.auth.adminPassword=<postgres-admin-password> \
  --set hub.database.password=<hub-db-password> \
  --set keycloak.database.password=<keycloak-db-password> \
  --set keycloak.admin.password=<keycloak-bootstrap-admin-password> \
  --set hub.admin.password=<hub-admin-password>
```

The Keycloak realm import is rendered from a dedicated template using:

- `keycloak.realmBootstrap.realmId`
- `hub.secrets.systemClientSecret` (optional; auto-generated when chart-managed Hub secret is used)
- `hub.admin.*` (realm-level Hub admin user; separate from `keycloak.admin.*` bootstrap user)

## Hub with External PostgreSQL and Keycloak

```bash
helm install hub charts/cryptomator-hub \
  --set keycloak.enabled=false \
  --set postgres.enabled=false \
  --set hub.database.jdbcUrl='jdbc:postgresql://db.example:5432/hub' \
  --set hub.database.username='hub' \
  --set hub.config.keycloakPublicUrl='https://sso.example/kc' \
  --set hub.config.keycloakLocalUrl='http://keycloak.svc.cluster.local:8080/kc' \
  --set hub.oidc.authServerUrl='http://keycloak.svc.cluster.local:8080/kc/realms/cryptomator' \
  --set hub.oidc.tokenIssuer='https://sso.example/kc/realms/cryptomator'
```

Even with `keycloak.enabled=false`, the chart still renders `realm.json` in Secret
`<release>-keycloak` so you can manually export/import it for your existing Keycloak.