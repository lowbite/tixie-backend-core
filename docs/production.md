# Production Configuration

The application is configured through Quarkus profiles and environment variables.

## Build Docker Image

Build the Quarkus JVM artifact first:

```shell
./gradlew build
```

Then build the Docker image:

```shell
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/tixie-core-backend-jvm .
```

## Configure Environment

Create a real production env file from the example:

```shell
cp .env.prod.example .env.prod
```

Update all `change_me` values before running. Do not commit `.env.prod`.

## Run Production Compose

```shell
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

The `app` service runs with:

```text
QUARKUS_PROFILE=prod
```

In the production profile the application requires:

```text
POSTGRES_USER
POSTGRES_PASSWORD
JDBC_URL
OIDC_AUTH_SERVER_URL
OIDC_CLIENT_ID
```

`docker-compose.prod.yml` builds `JDBC_URL` from the Postgres service name:

```text
jdbc:postgresql://postgres:5432/${POSTGRES_DB}
```

## Local Development

Use the default compose file for local dependencies:

```shell
docker compose up -d
```

Run the app in dev mode on the host:

```shell
./gradlew quarkusDev
```

The dev profile uses local defaults for Postgres and Keycloak.
