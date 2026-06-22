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

Default database values:

- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`
- `DB_JDBC_URL=jdbc:postgresql://localhost:5432/student_course_management`

You can override them by exporting environment variables before starting the app.

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

This starts both PostgreSQL and the app:

```bash
docker compose up --build
```

Services defined in `docker-compose.yml`:

- `postgres` on port `5432`
- `app` on port `8080`

Stop services:

```bash
docker compose down
```

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

## Reference

- Quarkus: https://quarkus.io/
- Hibernate ORM with Panache: https://quarkus.io/guides/hibernate-orm-panache
- Flyway: https://quarkus.io/guides/flyway
