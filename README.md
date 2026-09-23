# Netex Address Book

A Java 25 / Spring Boot address book with a React + TypeScript frontend.

**Current milestone: steps 1–3 implemented.** Docker Compose starts PostgreSQL, and the main API connects to it using Spring JDBC. Flyway creates the users and contacts tables on startup. The main API publicly lists and searches contacts, and retrieves one contact by ID. Both Java applications run independently, and the frontend checks the main API through a development proxy. Contact writes, authentication, Kafka and business HTTP interaction remain planned; Compose currently runs PostgreSQL only.

## Repository layout

```text
backend/           contacts-api: Spring Boot, Maven
frontend/          React, TypeScript, Vite
microservice/      activity-service: Spring Boot, Maven
docs/API.md        implemented and planned HTTP endpoints
docs/database.md   table inventory, relationship diagram and DBeaver walkthrough
docs/database.svg  visual database diagram (implemented backend schema)
docs/database.png  image preview of the database diagram
docs/database.drawio editable database diagram for draw.io / diagrams.net
compose.yaml       local PostgreSQL container and persistent volume
PROJECT_CONTEXT.md development decisions and progress (Romanian)
```

Each Java application has its own `pom.xml` and Maven Wrapper. Each app is started from its own directory.

## Requirements for local development

- JDK **25**. Confirm with `java -version`.
- Node.js **24 LTS** and npm. Confirm with `node -v` and `npm -v`.
- Internet access on the first build to download Maven and npm dependencies.

Maven **3.9.16** is downloaded by the committed wrapper; no separate Maven installation is required. Both services use Spring Boot **3.5.16**. Frontend dependency versions are recorded in `frontend/package-lock.json`.

Docker Desktop/Engine with Compose is required for PostgreSQL. Start the database before the main API. The activity service does not use a database yet. Backend integration tests also require Docker and create their own temporary PostgreSQL instance. For a visual database client, use DBeaver Community.

## Start PostgreSQL

Start Docker Desktop, then run from the repository root:

```powershell
Copy-Item .env.example .env
```

Do this only if `.env` does not already exist. Choose a local password in `.env`, then run:

```sh
docker compose up -d --wait postgres
```

On macOS/Linux, use `cp .env.example .env` for the initial copy. The root `.env` is ignored by Git and is separate from the optional frontend `.env`.

Connect with DBeaver to `localhost:5432`, database `netex`, user `netex`, and the password from the root `.env`. If you customize the database or user, use those values instead. See [the visual database walkthrough](docs/database.md).

The first backend startup applies `backend/src/main/resources/db/migration/V1__create_users_and_contacts.sql`. Refresh DBeaver afterwards to see `users`, `contacts` and `flyway_schema_history`. The application tables start empty; there are no demo users or contacts. The database username is a PostgreSQL account, separate from future address book user accounts.

`docker compose stop postgres` stops the database. The named volume keeps its data for the next start. Initialization variables only apply to an empty volume; editing the password in `.env` does not change an existing database password.

## Run locally

Open three terminals. The following commands assume each terminal starts at the repository root.

### Terminal 1: main API

PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

macOS / Linux:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Health endpoint: <http://localhost:8080/api/health>

Public contacts endpoint: <http://localhost:8080/api/contacts>. The local database starts with no contacts, so the response is `[]`. To search, use a URL such as <http://localhost:8080/api/contacts?name=Maria>. The `name` filter ignores case and matches part of a name. `GET /api/contacts/{id}` returns one contact or HTTP 404. See [the API contract](docs/API.md) for the response fields and validation rules.

The `local` Spring profile loads the root `.env` using `application-local.properties`. Run from `backend/`, so `../.env` points to the correct file. Use plain `KEY=value` lines in `.env`, without shell `export`, surrounding quotes or variable expansion. Generated hexadecimal passwords work with both Compose and Spring's properties reader.

Without the `local` profile (for example, when the backend later runs in Docker), supply `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD` as environment variables. Defaults are localhost, 5432, netex and netex; there is no default password. The standard `SPRING_DATASOURCE_URL` can override the complete JDBC URL. Compose's PostgreSQL port mapping remains 5432 unless you also change `compose.yaml`.

Flyway applies each migration once and validates it on later starts. Add a new migration for schema changes; do not edit an already applied migration. The main API now needs an available database to start, and its health check includes database connectivity. Database details remain hidden in the public response.

### Terminal 2: activity service

PowerShell:

```powershell
cd microservice
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```sh
cd microservice
./mvnw spring-boot:run
```

Health endpoint: <http://localhost:8081/internal/health>

This is currently a standalone service skeleton. It does not consume Kafka messages or receive contact activity yet.

### Terminal 3: frontend

```sh
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. The page should show **Server connected** when the main API is running. Use **Check again** after starting or restarting the backend.

`npm ci` is needed after cloning or changing dependencies, not before every run. Stop each application with Ctrl+C in its terminal.

## Ports and proxy

| Application | Default port | Configuration |
| --- | --- | --- |
| contacts-api | 8080 | `backend/src/main/resources/application.properties` |
| activity-service | 8081 | `microservice/src/main/resources/application.properties` |
| React / Vite | 5173 | `frontend/vite.config.ts` |

The browser requests `/api/health` from Vite on port 5173. Vite forwards `/api/*` to the main API on port 8080. The applications run in separate processes, while the browser uses one origin. This will also simplify session cookies later.

Vite refuses to silently switch ports when 5173 is occupied. Stop the conflicting process or change the port deliberately.

For an optional backend port override, set `SERVER_PORT` in that backend terminal. If the main API moves, copy `frontend/.env.example` to `frontend/.env`, update `API_PROXY_TARGET`, and restart Vite. No `.env` file is required with the defaults.

`npm run preview` only previews the built frontend files; this milestone configures the API proxy for `npm run dev`.

## IntelliJ IDEA and VS Code

Open `backend/` in IntelliJ IDEA and import its Maven project. Reload Maven after dependency changes and select JDK 25 as the Project SDK and Maven runner JRE. In **Run → Edit Configurations**, select the configuration for `ContactsApiApplication` and set:

- **Program arguments:** `--spring.profiles.active=local`
- **Working directory:** the absolute path of this repository's `backend` folder

If Program arguments is hidden, enable it through **Modify options**. Start PostgreSQL using Compose, then run the application. This works with a standard Java Application run configuration; a dedicated Spring UI is not required. The local profile reads the password from the ignored root `.env`.

Open `microservice/` as a second IntelliJ project and run `ActivityServiceApplication`. Open `frontend/` in VS Code and use its terminal for npm commands.

The build uses Maven and npm, not IDE-specific project files.

## Verify

Run in each Java application's directory:

```powershell
.\mvnw.cmd verify
```

On macOS / Linux use `./mvnw verify`.

Run in `frontend/`:

```sh
npm run lint
npm run build
```

The build includes TypeScript checking. `npm run typecheck` runs that check separately.

The Java integration tests verify the public health path, the contact read endpoints and that internal management information is not exposed. Spring Boot Actuator supplies the health endpoints; the contact endpoints use a controller, service and repository.

The backend tests use Testcontainers (test-only dependencies) with PostgreSQL 17.11 on a dynamically assigned port. They need Docker, but do not require a root `.env`, the `local` profile or the development database. The tests apply V1 to an empty database, check migration re-execution, identity/timestamp defaults, case-insensitive email uniqueness, the author foreign key, deletion restrictions and blank-name rejection. The contact API tests cover an empty list, ordered results, case-insensitive substring search, literal wildcard characters, invalid search length, one contact and an unknown ID. Test data is confined to that temporary instance. The container is removed after the test process ends.

With all apps running, check:

- Both Java health endpoints return HTTP 200 with `{"status":"UP"}`.
- <http://localhost:5173/api/health> returns the same response through Vite.
- `GET http://localhost:8080/api/contacts` returns HTTP 200 and `[]` before any contacts are created.
- The page reports a connected server. If you stop the main API and click **Check again**, it reports an unavailable server.

## Next milestone

Step 4 connects a first React contacts list and search form to these public endpoints. Contact writes and authentication follow in step 5. Photo paths are temporarily nullable until the upload feature is implemented. See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the full plan.
