# Developing the Helm chart

Installs the chart **from this checkout** (`/chart`) into a local [minikube](https://minikube.sigs.k8s.io/) cluster, optionally with a locally built Hub image. Use it when changing chart templates or verifying a Hub container build on Kubernetes.

## Prerequisites

- A checkout of this repository; all commands below run from its root.
- `kubectl` and Helm 3.
- A minikube cluster with the nginx ingress controller:

  ```bash
  minikube start
  minikube addons enable ingress
  ```

  Other local distributions (kind, Docker Desktop, k3d) work the same way once an nginx ingress controller is installed; only the `minikube image load` and `port-forward` commands below differ.

## Install from the local chart directory

```bash
helm install hub chart \
  --wait --timeout 5m \
  --set urls.kc.public=http://localhost:8080/kc \
  --set urls.hub.public=http://localhost:8080/hub \
  --set ingress.controller=nginx \
  --set hub.image.tag=2.0.0 \
  --set hub.admin.password=admin \
  --set hub.admin.passwordTemporary=false \
  --set keycloak.admin.password=admin \
  --set secrets.keepOnUninstall=true
```

- `hub.image.tag` must be set explicitly: the committed `Chart.yaml` carries a placeholder `appVersion`; the real one is injected by CI when the chart is packaged.
- `hub.admin.passwordTemporary=false` skips the forced password change on first login, so `admin` / `admin` keeps working across realm resets. Never use it in production.
- `secrets.keepOnUninstall=true` keeps the generated database passwords across `helm uninstall` / `helm install` cycles, so the persisted PostgreSQL volume stays usable while you iterate. Never use it in production.

Both services are exposed under one host with path prefixes, so a single ingress port serves everything. In a separate terminal, forward the ingress controller to `localhost:8080` and keep it running:

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80
```

| URL | Credentials |
|---|---|
| Hub: <http://localhost:8080/hub> | `admin` / `admin` |
| Keycloak admin console: <http://localhost:8080/kc> | `admin` / `admin` |

On first login Hub asks for a license. Any Hub license works for testing; see <https://cryptomator.org/hub/> on how to obtain one.

> [!NOTE]
> `kubectl port-forward` is used because it works with every minikube driver and needs no root privileges. If your cluster already exposes the ingress controller on the host — e.g. via `minikube tunnel` — skip the port-forward and use that address in the two `urls.*.public` values instead. The URLs are baked into the Keycloak realm on first boot, so decide before installing (see [Reset](#reset)).

## Using a locally built Hub image

Build the image (the Dockerfile only copies `backend/src`, so build the frontend into it first), load it into the cluster and point the chart at it. The image must match the cluster's architecture — minikube on Apple Silicon runs arm64 nodes.

```bash
(cd frontend && pnpm install && pnpm dist)
docker build -f backend/src/main/docker/Dockerfile.native -t ghcr.io/cryptomator/hub:dev backend
minikube image load ghcr.io/cryptomator/hub:dev
```

Add these to the install command above (or to `helm upgrade`), so the cluster uses the loaded image instead of pulling from the registry:

```bash
  --set hub.image.tag=dev \
  --set hub.imagePullPolicy=Never
```

After rebuilding under the same tag, run `minikube image load` again and restart the deployment: `kubectl rollout restart deployment/hub-hub`.

## Iterating on templates

```bash
helm lint chart
helm template hub chart --set urls.kc.public=http://localhost:8080/kc --set urls.hub.public=http://localhost:8080/hub --set ingress.controller=nginx | less
helm upgrade hub chart --reuse-values
```

Values are validated against `chart/values.schema.json`; `questions.yaml` drives the Rancher form. CI runs `helm lint` on every change under `chart/` (`.github/workflows/helm-chart.yml`).

## Reset

Keycloak imports the realm — including the redirect URIs derived from `urls.*.public` — **only on first boot**, and the database volume survives `helm uninstall`. After changing URLs or the realm template, reset the release completely:

```bash
helm uninstall hub
kubectl delete pvc data-hub-pg-0
kubectl delete secret hub-secrets-hub hub-secrets-kc hub-secrets-pg --ignore-not-found
```

Without a reset, stale redirect URIs surface as `Invalid parameter: redirect_uri` on login. To change URLs without losing data, edit the `cryptomatorhub` client in the Keycloak admin console instead.
