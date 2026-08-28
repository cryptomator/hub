[![CI Build](https://github.com/cryptomator/hub/actions/workflows/build.yml/badge.svg)](https://github.com/cryptomator/hub/actions/workflows/build.yml)

# Cryptomator Hub

Looking for a way to run Hub? See the [deployment options](deploy/README.md) — a self-contained Docker Compose file for local testing and a Helm chart for production.

Hub consists of these components:

## Web Frontend

During development, run vite from the `frontend` dir as explained in [its README file](frontend/README.md).

## Web Backend

During development, run Quarkus from the `backend` dir as explained in [its README file](backend/README.md).:

## Custom Keycloak Image

We add a custom theme to the base keycloak image, as explained in [its README file](keycloak/README.md).: