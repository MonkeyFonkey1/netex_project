# Netex Address Book

A Java 25 / Spring Boot address book with a React + TypeScript frontend.

**Current milestone: project setup (step 1).** Both Java applications can run independently. The frontend checks the main API through a development proxy. Contact management, authentication, SQL persistence, Kafka processing and the business HTTP interaction are planned next. The full Docker Compose setup has not been added yet.

## Repository layout

```text
backend/           contacts-api: Spring Boot, Maven
frontend/          React, TypeScript, Vite
microservice/      activity-service: Spring Boot, Maven
docs/API.md        implemented and planned HTTP endpoints
PROJECT_CONTEXT.md development decisions and progress (Romanian)
```

Each Java application has its own `pom.xml` and Maven Wrapper. Each app is started from its own directory.

## Requirements for local development

- JDK **25**. Confirm with `java -version`.
- Node.js **24 LTS** and npm. Confirm with `node -v` and `npm -v`.
- Internet access on the first build to download Maven and npm dependencies.

Maven **3.9.16** is downloaded by the committed wrapper; no separate Maven installation is required. Both services use Spring Boot **3.5.16**. Frontend dependency versions are recorded in `frontend/package-lock.json`.

Docker is not required for this milestone. PostgreSQL will be introduced through Docker Compose in step 2.

## Run locally

Open three terminals. The following commands assume each terminal starts at the repository root.

### Terminal 1: main API

PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```sh
cd backend
./mvnw spring-boot:run
```

Health endpoint: <http://localhost:8080/api/health>

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

Open `backend/` in IntelliJ IDEA and import its Maven project. Select JDK 25 as the Project SDK and Maven runner JRE, then run `ContactsApiApplication`.

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

The Java integration tests verify the public health path and that internal management information is not exposed. Spring Boot Actuator supplies these health endpoints; no custom health controller is needed.

With all apps running, check:

- Both Java health endpoints return HTTP 200 with `{"status":"UP"}`.
- <http://localhost:5173/api/health> returns the same response through Vite.
- The page reports a connected server. If you stop the main API and click **Check again**, it reports an unavailable server.

## Next milestone

Introduce PostgreSQL in Docker Compose, write Flyway SQL migrations for users and contacts, and connect the main API to the database. See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the full plan.
