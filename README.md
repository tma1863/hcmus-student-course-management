# HCMUS Student Course Management

Backend service for managing students and majors, built with Quarkus, PostgreSQL, Flyway, and Hibernate ORM (Panache).

## Tech stack

- Java 21
- Quarkus 3.36.3
- Maven Wrapper (`./mvnw`)
- PostgreSQL
- Flyway migrations
- Hibernate ORM + Panache

## Current project scope

The current codebase includes:

- Domain model for `Major` and `Student`
- Enum constraints for student `gender` and `status`
- Flyway migrations for database initialization
- A sample REST endpoint: `GET /hello`

## Prerequisites

- JDK 21
- Docker + Docker Compose (recommended for local database and app)

## Configuration

Application config is in `src/main/resources/application.properties`.

Environment variables are read from a local `.env` file (used by Docker Compose,
and by the app when run on the host). Create it from the template before your
first run:

```bash
cp .env.example .env
```

Default database values:

- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`
- `DB_JDBC_URL=jdbc:postgresql://localhost:5432/student_course_management`

`docker-compose.yml` reads `POSTGRES_*` and `APP_PORT` from `.env`; inside the
`app` container the `DB_*` values are overridden automatically to target the
`postgres` service. When running on the host, the `DB_*` values from `.env`
(or exported environment variables) are used instead.

## Run locally (dev mode)

Start Quarkus in live reload mode:

```bash
./mvnw compile quarkus:dev
```

Useful local URLs:

- App: `http://localhost:8080`
- Dev UI: `http://localhost:8080/q/dev/`
- Sample endpoint: `http://localhost:8080/hello`

## Run with Docker Compose

Compose is the recommended way to run everything. The `app` service builds from
`src/main/docker/Dockerfile.dev` (a Maven image) and runs Quarkus in **dev mode
with live reload**, bind-mounting the project into `/app`. The `postgres`
service holds the database.

Services defined in `docker-compose.yml`:

- `postgres` on port `5432`
- `app` on port `8080` (Quarkus dev mode, hot reload on source changes)

Common commands:

```bash
# Start both services, rebuilding the app image (foreground, streams logs)
docker compose up --build

# Start in the background
docker compose up --build -d

# Tail logs (all services, or just the app)
docker compose logs -f
docker compose logs -f app

# Open a shell inside the running app container
docker compose exec app bash

# Run a one-off Maven command inside the app container
docker compose exec app ./mvnw test

# Stop services (keeps the database volume)
docker compose down

# Stop services AND wipe the Postgres data volume (fresh DB next start)
docker compose down -v

# Rebuild the app image from scratch (e.g. after dependency changes)
docker compose build --no-cache app
```

> **Heads up — the container writes `target/` as root.** The `app` container runs
> as root and shares the project directory via a bind mount, so any build it
> performs (`quarkus:dev`, `spotless:apply`, `mvnw test`) creates `target/` files
> owned by `root` on the host. If you afterwards run `./mvnw` **directly on the
> host**, the build fails with `Operation not permitted` because your user can't
> overwrite root-owned files. See [Troubleshooting](#troubleshooting).

## Build and test

Run unit tests:

```bash
./mvnw test
```

Build package:

```bash
./mvnw package
```

Run packaged app:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Database migrations

Flyway scripts are in `src/main/resources/db/migration`:

- `V2026_06_18_0001__init.sql`: creates `majors` and `students`
- `V2026_06_18_0002__add_check_constraints_for_student_enums.sql`: adds enum check constraints

Migrations run automatically at startup via:

- `quarkus.flyway.migrate-at-start=true`

## Main source folders

- `src/main/java/com/example/studentcoursemanagement/common`
- `src/main/java/com/example/studentcoursemanagement/major`
- `src/main/java/com/example/studentcoursemanagement/student`
- `src/main/resources/db/migration`

## Troubleshooting

### `mvn`/`./mvnw` fails on the host with `Operation not permitted` copying to `target/`

Symptom (during `resources:resources` or `compile`):

```
Failed to copy .../target/classes/db/migration/....sql: Operation not permitted
```

**Cause:** the `app` container runs as root and bind-mounts the project, so a
build performed inside the container (`docker compose up`, `spotless:apply`,
`quarkus:dev`) leaves `target/` owned by `root`. A subsequent host-side `./mvnw`
runs as your user and cannot overwrite those files. Confirm with:

```bash
ls -ld target        # shows owner root instead of your user
```

**Fix — remove the root-owned build dir and rebuild as yourself:**

```bash
sudo rm -rf target && ./mvnw compile
```

Or, to keep the artifacts, just reclaim ownership:

```bash
sudo chown -R "$(id -u):$(id -g)" target
```

**Avoid it recurring:** pick one build path per session. Either build via the
container (`docker compose exec app ./mvnw ...`) **or** on the host (`./mvnw
...`) — don't mix them against the same `target/`. Note also that `./mvnw
quarkus:dev` on the host and `docker compose up` both bind to port `8080`, so
run only one at a time.

## Reference

- Quarkus: https://quarkus.io/
- Hibernate ORM with Panache: https://quarkus.io/guides/hibernate-orm-panache
- Flyway: https://quarkus.io/guides/flyway
