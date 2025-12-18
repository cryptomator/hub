# Cryptomator Hub Backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Dev Mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw clean quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Accessing Keycloak (Port 8180)

During development, Keycloak is started as a Quarkus Dev Service using port 8180. When using alternative ports, you can also find it via [http://localhost:8080/q/dev](http://localhost:8080/q/dev).


### Testing rest services via CLI:

First, access the keycloak admin web console and activate direct access grants for the `cryptomator` realm.

Then, retrieve an `access_token` from keycloak:

```
export access_token=$(\
    curl -X POST http://localhost:8180/auth/realms/cryptomator/protocol/openid-connect/token \
    --user admin:admin \
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

## Packaging

Make sure a container engine is running (required to register the built image locally).

Then run this command to build the image:

```shell script
docker build -f src/main/docker/Dockerfile.jvm -t ghcr.io/cryptomator/hub .
```

### Building native images

3x smaller but takes longer to build. Docker VM requires sufficient memory during the build:
```shell script
docker build -f src/main/docker/Dockerfile.native -t ghcr.io/cryptomator/hub .
```
