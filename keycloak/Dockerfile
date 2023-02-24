FROM quay.io/keycloak/keycloak:21.0.0 as builder
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/kc
ENV KC_FEATURES=web-authn
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:21.0.0
LABEL maintainer="info@skymatic.de"
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY /themes/cryptomator /opt/keycloak/themes/cryptomator
WORKDIR /opt/keycloak
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]