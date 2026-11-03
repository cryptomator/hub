FROM quay.io/keycloak/keycloak:26.8.1@sha256:a78d6b79572168cb25886b80a8b5b8d4ed4390743e3eecb810538683e209dd6b AS builder
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=web-authn
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build

# this block is required because curl isn't in the keycloak image anymore, but is needed for the Docker Compose health check.
FROM registry.access.redhat.com/ubi9:9.8@sha256:8bf0e8f20737e9c8a68c8a498299e9504ab397b1b1f2837acb2fef12ec698f0e AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs curl --releasever 9 --setopt install_weak_deps=false --nodocs -y; dnf --installroot /mnt/rootfs clean all

FROM quay.io/keycloak/keycloak:26.8.1@sha256:a78d6b79572168cb25886b80a8b5b8d4ed4390743e3eecb810538683e209dd6b
LABEL maintainer="info@skymatic.de"
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY --from=ubi-micro-build /mnt/rootfs /
COPY /themes/cryptomator /opt/keycloak/themes/cryptomator

# distrust the CA root affected by the eMudhra key compromise (2026-10-17), the distro bundle may not have dropped it yet.
# The JVM's cacerts is a symlink to /etc/pki/ca-trust/extracted/java/cacerts, so update-ca-trust covers Keycloak's trust store, too.
USER root
RUN trust extract --format=pem-bundle --filter="pkcs11:object=emSign%20ECC%20Root%20CA%20-%20G3" --overwrite /etc/pki/ca-trust/source/blocklist/emsign-ecc-root-ca-g3.pem \
  && update-ca-trust
USER 1000

WORKDIR /opt/keycloak
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
