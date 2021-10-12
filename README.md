# Cryptomator Hub

Hub consists of these components:

## Keycloak (Port 8080)

Keycloak handles user authentication.

During development, run this from `keycloak` dir:

```shell
docker run --rm -p 8080:8080 \
-e KEYCLOAK_USER=admin \
-e KEYCLOAK_PASSWORD=admin \
-e KEYCLOAK_IMPORT=/cfg/cryptomator-dev-realm.json \
-v $(pwd):/cfg:ro \
quay.io/keycloak/keycloak:15.0.2 # arm64: mihaibob/keycloak:15.0.1
```
When working with powershell, run this instead of the above from the `keycloak` dir:
```powershell
docker run --rm -p 8080:8080 `
-e KEYCLOAK_USER=admin `
-e KEYCLOAK_PASSWORD=admin `
-e KEYCLOAK_IMPORT=/cfg/cryptomator-dev-realm.json `
-v ${PWD}:/cfg:ro `
quay.io/keycloak/keycloak:15.0.2
```

## Web Frontend (Port 3000)

During development, run this from `web` dir:

```shell
npm run dev
```

## Web Backend (Port 9090)

During development, run this from `spi` dir:

```shell
mvn compile quarkus:dev
```

### Testing rest services via CLI:

First, access the keycloak admin web console and activate direct access grants for the `cryptomator-hub` realm.

Then, retrieve an `access_token` from keycloak:

```
export access_token=$(\
    curl -X POST http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/token \
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
