# Cryptomator Hub

[Cryptomator Hub](https://cryptomator.org/hub/) brings [Cryptomator](https://cryptomator.org/)'s client-side encryption to teams. It manages who may unlock which vault — for users and groups from your identity provider — without ever seeing a vault's key: keys are encrypted for each authorized user in the browser, and the server only stores and distributes those encrypted envelopes. Hub adds emergency access, a web of trust between users, device management and an audit log on top, and the Cryptomator desktop and mobile apps unlock Hub vaults directly.

Hub is licensed per user; see <https://cryptomator.org/pricing/> for plans. A license is entered on first login, and you can evaluate Hub before purchasing one.

## What this chart deploys

| Component | Image | Bundled by default | Notes |
|---|---|---|---|
| Cryptomator Hub | `ghcr.io/cryptomator/hub` | always | The application itself; runs as a native binary. |
| Keycloak | `ghcr.io/cryptomator/keycloak` | yes, optional | Identity provider. Users, groups and logins are managed here; the chart creates Hub's realm on first start. Can be replaced by a Keycloak you already operate. |
| PostgreSQL | `postgres` | yes, optional | Database for Hub and Keycloak. Can be replaced by an existing server (two databases required if Keycloak is bundled). |

The chart relies on an **nginx** or **Traefik** ingress controller in your cluster and on two public URLs — one for Hub, one for Keycloak — either as separate hostnames (`hub.example.com`, `kc.example.com`) or as paths on one host (`example.com/hub`, `example.com/kc`). TLS is terminated by the ingress controller; the chart can attach an existing certificate or request one through cert-manager.

Passwords you don't set are generated on install and stored in Kubernetes Secrets; the install notes show how to retrieve them.

> [!IMPORTANT]
> Keycloak creates Hub's realm — including the public URLs — only on its **first** start. Decide on the final URLs before installing; changing them later means editing the `cryptomatorhub` client in the Keycloak admin console or reinstalling with a fresh database.

## Installing

**Rancher:** add `oci://ghcr.io/cryptomator/charts` as a repository (*Apps → Repositories*, Rancher 2.9+) and install *Cryptomator Hub* from *Apps → Charts*. The form asks only for the URLs, the ingress controller, an optional certificate and the admin passwords; untick *Deploy bundled PostgreSQL* / *Deploy bundled Keycloak* to connect to existing services instead.

**Helm CLI:**

```bash
helm install hub oci://ghcr.io/cryptomator/charts/cryptomator-hub \
  --namespace cryptomator --create-namespace \
  --set urls.hub.public=https://hub.example.com \
  --set urls.kc.public=https://kc.example.com \
  --set ingress.controller=nginx
```

All values are documented in `values.yaml` and validated against `values.schema.json`. Step-by-step guides — local clusters, connecting an existing Keycloak, upgrading — are in the [deployment docs](https://github.com/cryptomator/hub/tree/develop/deploy).

## Verify Published Chart

This chart contains a OCI chart signature, which can be verified as follows (assuming chart version `2.0.1`):

```bash
cosign verify \
  --certificate-identity-regexp 'https://github.com/cryptomator/hub/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/cryptomator/charts/cryptomator-hub:2.0.1
```

You can additionally inspect provenance attestations:

```bash
cosign verify-attestation \
  --type https://slsa.dev/provenance/v1 \
  --certificate-identity-regexp 'https://github.com/cryptomator/hub/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/cryptomator/charts/cryptomator-hub:2.0.1
```
