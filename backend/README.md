# Cryptomator Hub Backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Dev Mode

You can run your application in dev mode that enables live coding using:
```shell script
mvn clean quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging

Make sure Docker is running (required to register the built image locally).

Then run this command to build the image:
```shell script
mvn clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.tag=latest
```

Or to build a native image (3x smaller, takes longer to build):
```shell script
mvn clean package -Pnative -Dquarkus.container-image.build=true -Dquarkus.native.container-build=true -Dquarkus.container-image.tag=latest
```
