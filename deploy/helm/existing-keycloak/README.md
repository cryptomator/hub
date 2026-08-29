# Helm with an existing Keycloak

Deploys Hub (and the bundled PostgreSQL) from the published chart, but connects it to a Keycloak instance you already operate — e.g. your organisation's SSO. The chart then does **not** create the Keycloak realm for you: you have to import Hub's `realm.json` into your Keycloak once.

Requirements: Kubernetes ≥ 1.27 with an nginx or Traefik ingress controller, `kubectl`, Helm 3.8+, and admin access to a Keycloak 26.x instance reachable from both the browser and the cluster.

1. Install the chart with your own values:

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

   | Value | Meaning |
   |---|---|
   | `keycloak.enabled=false` | Do not deploy the bundled Keycloak |
   | `urls.kc.public` | Public URL of your Keycloak |
   | `urls.kc.clusterInternal` | Optional URL Hub uses to reach Keycloak from inside the cluster, if it differs from the public one (e.g. `http://keycloak.sso.svc.cluster.local:8080`) |
   | `urls.hub.public` | Public URL of Hub; becomes the redirect URI of the `cryptomatorhub` client in the realm |
   | `ingress.controller` | `nginx` or `traefik` |
   | `ingress.certificate.clusterIssuer` / `.secretName` | Optional cert-manager issuer or existing TLS secret; leave both empty when TLS is terminated in front of the cluster |
   | `hub.secrets.systemClientSecret` | Secret of the `cryptomatorhub-system` client, used by Hub to sync users and groups; generated if unset |
   | `hub.admin.password` | Initial password of Hub's `admin` user; generated if unset, must be changed on first login |
   | `hub.config.keycloakRealm` | Realm name, default `cryptomator` |

   All other values are documented in [`values.yaml`](../../../chart/values.yaml). The Hub pod will not become ready yet: its `wait-for-oidc` init container waits for the realm to exist.
2. Import `realm.json` into your Keycloak. The chart renders it with your `--set` values and stores it in the release's Secret:

   ```bash
   kubectl get secret -n cryptomator hub-secrets-kc -o jsonpath='{.data.realm\.json}' | base64 -d > realm.json
   ```

   In the Keycloak admin console, open the realm selector at the top left, click **Create realm**, use **Browse…** to select `realm.json`, and click **Create**. It contains the realm roles `user`, `create-vaults`, `admin`, the clients `cryptomatorhub`, `cryptomator` and `cryptomatorhub-system`, and the `admin` user.

   > [!WARNING]
   > `realm.json` contains the `cryptomatorhub-system` client secret and the Hub admin password in clear text. Delete the file after the import.
3. Wait until the Hub pod is `Running` (`kubectl get pods -n cryptomator -w`), then open `https://hub.example.com` and sign in as `admin`.

For everything else — license, user management, login theme, backups, upgrades — see <https://docs.cryptomator.org/hub/>.
