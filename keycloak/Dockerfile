FROM quay.io/keycloak/keycloak:26.7.4@sha256:82a77884f3af238beab1e7afd63b5f530e1b5c0590bd7aa60b40a40463e29b2c AS builder
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=web-authn
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build

# this block is required because curl isn't in the keycloak image anymore, but is needed for the Docker Compose health check.
FROM registry.access.redhat.com/ubi9:9.8@sha256:8bf0e8f20737e9c8a68c8a498299e9504ab397b1b1f2837acb2fef12ec698f0e AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs curl --releasever 9 --setopt install_weak_deps=false --nodocs -y; dnf --installroot /mnt/rootfs clean all

FROM quay.io/keycloak/keycloak:26.7.4@sha256:82a77884f3af238beab1e7afd63b5f530e1b5c0590bd7aa60b40a40463e29b2c
LABEL maintainer="info@skymatic.de"
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY --from=ubi-micro-build /mnt/rootfs /
COPY /themes/cryptomator /opt/keycloak/themes/cryptomator
WORKDIR /opt/keycloak
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
