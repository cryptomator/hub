# Cryptomator Hub

Hub consists of these components:

## Keycloak (Port 8080)

Keycloak handles user authentication.

For development run it using:

```shell
docker run --rm -p 8080:8080 \
-e KEYCLOAK_USER=admin \
-e KEYCLOAK_PASSWORD=admin \
-e KEYCLOAK_IMPORT=/cfg/cryptomator-public-realm.json \
-v $(pwd)/keycloak:/cfg:ro \
quay.io/keycloak/keycloak:14.0.0
```

Optionally retrieve an `access_token` for further tests:

```
export access_token=$(\
    curl -X POST http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/token \
    --user cryptomator-hub:CHANGEME \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=owner&password=owner&grant_type=password' | jq --raw-output '.access_token' \
)
```

## DB ?

TODO maybe not required?


## SPI (Port 9090)

Web services.

Run using:

```shell
mvn -fspi/pom.xml compile quarkus:dev
# or
# docker run --rm -p 9090:9090 cryptomator/hub-spi:latest
```

Test services:

```shell
curl -v -X GET \
  http://localhost:9090/users/me \
  -H "Authorization: Bearer "$access_token
```


## Web

Static web frontend - maybe included in spi as static resource...?