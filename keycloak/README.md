# Custom Keycloak Image

This custom keycloak image adds a Cryptomator theme and sets some build-time configuration values.

## Theme Development

For local testing, build the changed theme

```shell script
cd themes/cryptomator/common/resources
npm install
npm run build
```

and then create the docker image.

### Live Coding

For live coding, start Keycloak in dev mode (to disable theme caching) and mounting the theme folder as a volume:

```shell script
docker run --rm -it -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -v $(pwd)/themes/cryptomator:/opt/keycloak/themes/cryptomator:ro quay.io/keycloak/keycloak:19.0 start-dev
```

Using a different shell, build the web components:

```shell script
cd themes/cryptomator/common/resources
npm install
npm run dev
```

The Cryptomator Theme is [based on the Base theme](https://github.com/keycloak/keycloak/tree/main/themes/src/main/resources/theme/base). Use it as a reference.

## Release Builds

Release builds are created for amd64 and arm64 by triggering [this GitHub Workflow](https://github.com/cryptomator/hub/actions/workflows/keycloak.yml).

Please use a tag based on the base image, so we can easily know that e.g. `ghcr.io/cryptomator/keycloak:19.0.1.4` is based on `quay.io/keycloak/keycloak:19.0.1`.