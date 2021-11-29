[![Build and test](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml/badge.svg)](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml)

# Cryptomator Hub

Hub consists of these components:

## Web Frontend (Port 3000)

During development, run this from `frontend` dir:

```shell
npm run dev
```

## Web Backend (Port 9090)

During development, start Docker, then run this from `backend` dir:

```shell
mvn clean quarkus:dev
```

Or on ARM64:

```shell
mvn clean quarkus:dev -Dquarkus.keycloak.devservices.image-name=mihaibob/keycloak:15.0.1
```

### Accessing Keycloak

Keycloak is started by Quarkus as a "Dev Service" on a system-assigned port. To access dev services, visit [http://localhost:9090/q/dev](http://localhost:9090/q/dev).

### Testing rest services via CLI:

First, access the keycloak admin web console and activate direct access grants for the `cryptomator-hub` realm.

Then, retrieve an `access_token` from keycloak:

```
export access_token=$(\
    curl -X POST http://localhost:port/auth/realms/cryptomator/protocol/openid-connect/token \
    --user cryptomator-hub:CHANGEME \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=owner&password=owner&grant_type=password' | jq --raw-output '.access_token' \
)
```

Then use this token as a Bearer Token:

```shell
curl -v -X GET \
  http://localhost:9090/users/me \
  -H "Authorization: Bearer "$access_token
```
