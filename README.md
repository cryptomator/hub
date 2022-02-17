[![Build and test](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml/badge.svg)](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml)

# Cryptomator Hub

Hub consists of these components:

## Web Frontend (Port 3000)

During development, run this from `frontend` dir:

```shell
npm run dev
```

## Web Backend (Port 8080)

During development, start Docker, then run this from `backend` dir:

```shell
mvn clean quarkus:dev
```

Or on ARM64:

```shell
mvn clean quarkus:dev -Dquarkus.keycloak.devservices.image-name=mihaibob/keycloak:15.0.1
```

### Accessing Keycloak (Port 8180)

During development, Keycloak is started as a Quarkus Dev Service using port 8180. When using alternative ports, you can also find it via [http://localhost:8080/q/dev](http://localhost:8080/q/dev).

### Testing rest services via CLI:

First, access the keycloak admin web console and activate direct access grants for the `cryptomator-hub` realm.

Then, retrieve an `access_token` from keycloak:

```
export access_token=$(\
    curl -X POST http://localhost:8180/auth/realms/cryptomator/protocol/openid-connect/token \
    --user cryptomator-hub:CHANGEME \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=owner&password=owner&grant_type=password' | jq --raw-output '.access_token' \
)
```

Then use this token as a Bearer Token:

```shell
curl -v -X GET \
  http://localhost:8080/users/me \
  -H "Authorization: Bearer "$access_token
```
