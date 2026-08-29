# Helm with an existing Keycloak

Deploys Hub (and the bundled PostgreSQL) from the published chart, but connects it to a Keycloak instance you already operate — e.g. your organisation's SSO. The chart then does **not** create the Keycloak realm for you: you have to import Hub's `realm.json` into your Keycloak once, as described below.

Requirements: a Kubernetes cluster with an nginx or Traefik ingress controller, `kubectl`, Helm 3.8+, and admin access to a Keycloak 26.x instance reachable from both the browser and the cluster.

## 1. Install Hub

Choose a client secret for the `cryptomatorhub-system` client — Hub uses it to sync users and groups from Keycloak — and pass it to the chart along with the public Keycloak URL:

```bash
helm install hub oci://ghcr.io/cryptomator/charts/cryptomator-hub --version 2.0.0 \
  --namespace cryptomator --create-namespace \
  --set keycloak.enabled=false \
  --set urls.kc.public=https://sso.example.com \
  --set urls.hub.public=https://hub.example.com \
  --set ingress.controller=nginx \
  --set hub.secrets.systemClientSecret="$(openssl rand -hex 32)" \
  --set hub.admin.password=change-me
```

Optional values:

- `urls.kc.clusterInternal` — URL Hub uses to reach Keycloak from inside the cluster, if it differs from the public one (e.g. `http://keycloak.sso.svc.cluster.local:8080`). The token issuer stays derived from `urls.kc.public`.
- `hub.config.keycloakRealm` — realm name, default `cryptomator`.
- `ingress.certificate.secretName` / `ingress.certificate.clusterIssuer` — attach an existing TLS secret or let cert-manager request a certificate; leave both empty when your ingress controller or load balancer already terminates TLS (see the comments in [`values.yaml`](../../../chart/values.yaml)).

The Hub pod will not become ready yet: its `wait-for-oidc` init container polls `<keycloak>/realms/<realm>/.well-known/openid-configuration` until the realm exists. That is expected — continue with the import.

## 2. Import `realm.json` into your Keycloak

The chart renders the realm the way it would for a bundled Keycloak and stores it in the release's Secret, so the client secret, redirect URIs and admin user in the file already match your `--set` values:

```bash
kubectl get secret -n cryptomator hub-secrets-kc -o jsonpath='{.data.realm\.json}' | base64 -d > realm.json
```

> [!WARNING]
> `realm.json` contains the `cryptomatorhub-system` client secret and the Hub admin password in clear text. Delete the file after the import.

Import it in the Keycloak admin console: open the realm selector at the top left, click **Create realm**, use **Browse…** to select `realm.json`, and click **Create**. (Alternatively, on the Keycloak host: `kc.sh import --file realm.json` before starting the server.)

The imported realm contains:

| Item | Purpose |
|---|---|
| Realm roles `user`, `create-vaults`, `admin` | Mapped to Hub permissions; `user` is the default role for new users |
| Client `cryptomatorhub` (public, PKCE) | Used by the Hub web UI; its redirect URI is `<urls.hub.public>/*` |
| Client `cryptomator` (public, PKCE) | Used by the Cryptomator desktop and mobile apps |
| Client `cryptomatorhub-system` (confidential) + service account `system` with `realm-management` roles | Used by Hub's backend to sync users and groups; the secret must equal `hub.secrets.systemClientSecret` |
| User `admin` with role `admin` | Initial Hub administrator (`hub.admin.username` / `hub.admin.password`) |

Once the realm exists, the init container finishes and Hub becomes ready:

```bash
kubectl get pods -n cryptomator -w
```

Open `urls.hub.public`, sign in as `admin` and enter your license. Users you add to the realm (or federate into it via LDAP / an identity broker) appear in Hub within `hub.config.keycloakSyncerPeriod` (default 5 minutes) as long as they hold the `user` role.

## Notes

- **Login theme**: the realm sets `loginTheme: cryptomator`, which only exists in the `ghcr.io/cryptomator/keycloak` image. A stock Keycloak logs a warning and falls back to its built-in theme; either clear the login theme under *Realm settings → Themes*, or copy `keycloak/themes/cryptomator` from this repository into your Keycloak's `themes/` directory.
- **Cross-origin**: Keycloak and Hub live on different origins in this setup. The chart adds the Keycloak origin to Hub's `Content-Security-Policy` (`connect-src`) automatically; nothing to configure.
- **Rotating the client secret**: change it in both places — `--set hub.secrets.systemClientSecret=…` on `helm upgrade` and *Clients → cryptomatorhub-system → Credentials* in Keycloak.
- **Changing `urls.hub.public` later**: `helm upgrade` updates Hub, but the redirect URI of the `cryptomatorhub` client in Keycloak has to be edited by hand, otherwise login fails with `Invalid parameter: redirect_uri`.
