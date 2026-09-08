# Docker Compose in production

Runs Cryptomator Hub with bundled Keycloak and PostgreSQL on a single Docker host, behind [Traefik](https://traefik.io/) with Let's Encrypt certificates. [`compose.yaml`](compose.yaml) is the only file you need; the comments in it explain what to change and how to swap in an existing proxy, PostgreSQL or Keycloak.

Requirements: Docker Compose ≥ 2.23.1, a host reachable from the internet on ports 80 and 443, DNS records for the two hostnames.

1. Replace all occurrences of the following placeholders in `compose.yaml`:

   | Placeholder | Meaning | Used in |
   |---|---|---|
   | `hub.example.com` | Public hostname of Hub | Traefik router, realm redirect URI, realm `frame-ancestors` |
   | `kc.example.com` | Public hostname of Keycloak | Traefik router, `KC_HOSTNAME`, `HUB_KEYCLOAK_PUBLIC_URL`, `QUARKUS_OIDC_TOKEN_ISSUER`, Hub's CSP `connect-src` |
   | `admin@example.com` | Contact address for Let's Encrypt | Traefik ACME resolver |
   | `CHANGE-ME-HUB-ADMIN-PASSWORD` | Initial password of Hub's `admin` user; must be changed on first login | realm |
   | `CHANGE-ME-KEYCLOAK-ADMIN-PASSWORD` | Password of Keycloak's bootstrap admin `admin` | `KC_BOOTSTRAP_ADMIN_PASSWORD` |
   | `CHANGE-ME-SYSTEM-CLIENT-SECRET` | Secret of the `cryptomatorhub-system` client, used by Hub to sync users and groups | `HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET`, realm |
   | `CHANGE-ME-HUB-DB-PASSWORD` | Password of the `hub` database user | `POSTGRES_PASSWORD`, `QUARKUS_DATASOURCE_PASSWORD` |
   | `CHANGE-ME-KEYCLOAK-DB-PASSWORD` | Password of the `keycloak` database user | `KC_DB_PASSWORD`, `create-keycloak-db` config |

   Generate secrets with `openssl rand -hex 32`. Use search & replace — most values occur more than once and all occurrences must be identical.
2. `docker compose up -d`
3. Wait until `docker compose ps` shows all services as `healthy`, then open `https://hub.example.com` and sign in as `admin`.

For everything else — license, user management, backups, upgrades — see <https://docs.cryptomator.org/hub/>.
