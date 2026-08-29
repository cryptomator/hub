# Cryptomator Hub

[Cryptomator Hub](https://cryptomator.org/hub/) adds zero-knowledge key management for teams to Cryptomator: vault keys are shared with users and groups without ever leaving the client unencrypted.

By default this chart deploys the complete stack: Hub, [Keycloak](https://www.keycloak.org/) as identity provider and a PostgreSQL database. Untick *Deploy bundled PostgreSQL* / *Deploy bundled Keycloak* to connect to an existing database or Keycloak instead.

Before installing you need: two public URLs for Hub and Keycloak (either two hostnames, or one hostname with two paths such as `/hub` and `/kc`), and an nginx or Traefik ingress controller in the cluster. For HTTPS, either your ingress controller already terminates TLS, or you tick *Attach a certificate* and name a cert-manager ClusterIssuer or an existing TLS secret. Hub asks for a license on first login; see <https://cryptomator.org/hub/> on how to obtain one.

The detailed description below explains what Hub is, which components the chart deploys and how to verify the chart's signature.
