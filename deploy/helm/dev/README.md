# Developing the Helm chart

Installs the chart **from this checkout** (`/chart`) into a local [minikube](https://minikube.sigs.k8s.io/) cluster with a locally built Hub image. Use it when changing chart templates or verifying a Hub container build on Kubernetes.

Prerequisites:

- A checkout of this repository; all commands below run from its root.
- `kubectl`, Helm 3, Docker, `pnpm`.
- A minikube cluster with the nginx ingress controller: `minikube start && minikube addons enable ingress`. Other local distributions (kind, Docker Desktop, k3d) work the same way once an nginx ingress controller is installed; only `minikube image load` and the `port-forward` target differ.

1. Build the frontend, then the backend as a native image (the Dockerfile only copies `backend/src`, so the frontend must be built into it first), and load it into the cluster:

   ```bash
   (cd frontend && pnpm install && pnpm dist)
   docker build -f backend/src/main/docker/Dockerfile.native -t ghcr.io/cryptomator/hub:dev backend
   minikube image load ghcr.io/cryptomator/hub:dev
   ```

   The image must match the cluster's architecture — minikube on Apple Silicon runs arm64 nodes.
2. Install the chart from the local directory:

   ```bash
   helm install hub chart \
     --wait --timeout 5m \
     --set urls.kc.public=http://localhost:8080/kc \
     --set urls.hub.public=http://localhost:8080/hub \
     --set ingress.controller=nginx \
     --set hub.image.tag=dev \
     --set hub.imagePullPolicy=Never \
     --set hub.admin.password=admin \
     --set hub.admin.passwordTemporary=false \
     --set keycloak.admin.password=admin \
     --set secrets.keepOnUninstall=true
   ```

   | Value | Meaning |
   |---|---|
   | `urls.hub.public`, `urls.kc.public` | Both services under one host with path prefixes, so a single forwarded ingress port serves everything. Baked into the Keycloak realm on first boot — see Reset below when changing them |
   | `hub.image.tag=dev`, `hub.imagePullPolicy=Never` | Use the image loaded in step 1 instead of pulling from the registry. To test a published image instead, set the tag to a release (e.g. `2.0.0`) and drop the pull policy; the committed `Chart.yaml` only carries a baseline `appVersion` that the release workflow overrides |
   | `hub.admin.password`, `hub.admin.passwordTemporary=false` | Fixed `admin` / `admin` login without forced password change. Never in production |
   | `keycloak.admin.password` | Keycloak bootstrap admin `admin` / `admin` |
   | `secrets.keepOnUninstall=true` | Keeps the generated database passwords across `helm uninstall` / `helm install`, so the persisted PostgreSQL volume stays usable. Never in production |
3. In a separate terminal, forward the ingress controller and keep it running:

   ```bash
   kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80
   ```

   Hub: <http://localhost:8080/hub>, Keycloak admin console: <http://localhost:8080/kc>, both `admin` / `admin`.

**Iterating** — after rebuilding the image under the same tag: `minikube image load ghcr.io/cryptomator/hub:dev && kubectl rollout restart deployment/hub-hub`. After changing templates: `helm lint chart`, then `helm upgrade hub chart --reuse-values`.

**Reset** — the realm (incl. redirect URIs) is imported on Keycloak's first boot only and the database volume survives `helm uninstall`, so after changing URLs or the realm template start over completely:

```bash
helm uninstall hub
kubectl delete pvc data-hub-pg-0
kubectl delete secret hub-secrets-hub hub-secrets-kc hub-secrets-pg --ignore-not-found
```
