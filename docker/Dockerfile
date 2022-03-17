FROM quay.io/keycloak/keycloak:17.0.0
LABEL maintainer="martin.beyer@skymatic.de"

ENV LANG=C.UTF-8
# ENV KEYCLOAK_DEFAULT_THEME=cryptomatorTheme
COPY /themes/cryptomatorTheme opt/keycloak/themes/cryptomatorTheme
# With version change from keycloak 15.0.2 to 17.0.0 the location of themes changed
# https://www.keycloak.org/docs/latest/server_development/index.html#_themes
# https://www.keycloak.org/docs/latest/server_installation/index.html#set-the-global-default-theme setting global theme
# 1st build with `docker build -t native-keycloak .`
# 2nd run with `docker run -it nativ-keycloak`
# docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -it native-keycloak start-dev

# todo have the theme pre-selected

#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--spi-theme-welcome-theme=cryptomatorTheme"]
# docker build . -t prebuilt_keycloak
# docker run --name keycloak_test -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -e KEYCLOAK_DEFAULT_THEME=cryptomatorTheme -it my_keycloak start-dev --rm
