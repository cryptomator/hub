# Cryptomator Hub Helm Chart

This chart deploys:

- Cryptomator Hub (required)
- Keycloak (optional, enabled by default)
- PostgreSQL (optional, enabled by default)

TLS termination is currently expected to be done by ingress controller.


## Quick Start (Full Internal Stack)

```bash
helm install hub charts/cryptomator-hub \
  --namespace default \
  --set ingress.enabled=true \
  --set global.host=domain.tld \
  --set postgres.auth.adminPassword=<postgres-admin-password> \
  --set postgres.auth.hubPassword=<hub-db-password> \
  --set postgres.auth.keycloakPassword=<keycloak-db-password> \
  --set keycloak.admin.password=<keycloak-bootstrap-admin-password> \
  --set hub.admin.password=<hub-admin-password>
```

The Keycloak realm import is rendered from a dedicated template using:

- `keycloak.realmBootstrap.realmId`
- `keycloak.realmBootstrap.publicHost` (falls back to `global.host`)
- `hub.secrets.data.systemClientSecret` (optional; auto-generated when chart-managed Hub secret is used)
- `keycloak.realmBootstrap.systemClientSecret` (optional override for realm template)
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
  --set hub.oidc.tokenIssuer='https://sso.example/kc/realms/cryptomator' \
  --set hub.secrets.existingSecret='hub-external-secrets' \
  --set keycloak.realmBootstrap.systemClientSecret='<system-client-secret>'
```

Even with `keycloak.enabled=false`, the chart still renders `realm.json` in ConfigMap
`<release>-keycloak-realm` so you can manually import it into your existing Keycloak.

## Secret Keys

When using `hub.secrets.existingSecret`, the secret must contain:

- `hub-system-client-secret` (or custom `hub.secrets.keys.systemClientSecret`)
- `hub-db-password` (or custom `hub.secrets.keys.dbPassword`, only required when `postgres.enabled=false`)
- `hub-initial-id` (optional)
- `hub-initial-license` (optional)

If you use `hub.secrets.existingSecret`, also set `keycloak.realmBootstrap.systemClientSecret` unless the existing secret is already present in-cluster at render time.
