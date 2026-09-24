# Netex Address Book

A Java 25 / Spring Boot address book with a React + TypeScript frontend.

**Current milestone: step 5A.1 backend implemented.** Docker Compose starts PostgreSQL. Flyway creates the users and contacts tables and adds account roles. The main API publicly lists and searches contacts. The backend supports signup, login, logout, sessions and CSRF protection. React displays the public list; its account and admin pages are still placeholders until step 5A.2. Contact writes, Kafka, the admin activity API and business HTTP interaction remain planned; Compose currently runs PostgreSQL only.

## Repository layout

```text
backend/           contacts-api: Spring Boot, Maven
frontend/          React, TypeScript, Vite
frontend/src/features/  contact UI and future auth/admin UI, grouped by feature
frontend/src/navigation/  client-side routes and shared header
microservice/      activity-service: Spring Boot, Maven
docs/API.md        implemented and planned HTTP endpoints
docs/database.md   table inventory, relationship diagram and DBeaver walkthrough
docs/database.svg  visual database diagram (implemented backend schema)
docs/database.png  image preview of the database diagram
docs/database.drawio editable database diagram for draw.io / diagrams.net
output/pdf/pasul-1-baza-de-date-si-inregistrare.pdf  learning guide for database and signup (Romanian)
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

The backend applies `V1__create_users_and_contacts.sql` and then `V2__add_user_role.sql`. Refresh DBeaver afterwards to see `users`, `contacts` and `flyway_schema_history`; the `users` table now has a `role` column. There are no demo users or contacts. The database username is a PostgreSQL account, separate from address book user accounts.

To create an optional admin account, set both `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the ignored root `.env` before starting the backend. The password needs 8–72 UTF-8 bytes. The backend creates the admin only if that email does not exist. It will not promote an existing regular user or reset an existing admin password. Never commit real credentials.

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

### Try the authentication backend

These commands use PowerShell and the backend on port 8080. Each request reuses the same `WebRequestSession`, which holds the `JSESSIONID` cookie. Choose an email you have not registered before:

```powershell
$browserSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $browserSession
$headers = @{}
$headers[$csrf.headerName] = $csrf.token
$body = @{ email = 'ana@example.com'; password = 'a-long-password' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/auth/signup -Method Post -WebSession $browserSession -Headers $headers -ContentType 'application/json' -Body $body
$loginForm = @{ email = 'ana@example.com'; password = 'a-long-password' }
(Invoke-WebRequest http://localhost:8080/api/auth/login -Method Post -WebSession $browserSession -Headers $headers -ContentType 'application/x-www-form-urlencoded' -Body $loginForm).StatusCode
Invoke-RestMethod http://localhost:8080/api/auth/me -WebSession $browserSession
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $browserSession
$headers[$csrf.headerName] = $csrf.token
Invoke-RestMethod http://localhost:8080/api/auth/logout -Method Post -WebSession $browserSession -Headers $headers
```

Signup creates only a `USER` and does not log in automatically. Login is handled by Spring Security, accepts form fields and returns HTTP 204 with no body; use `/api/auth/me` to read the account. Login changes the session ID and CSRF token, so fetch a fresh token before logout. `GET /api/auth/me` returns HTTP 401 after logout. The backend hashes passwords using BCrypt and returns only `id`, `email` and `role` from signup and `/me`. See [the API contract](docs/API.md) for validation and status codes. Publishing a Kafka signup event is a later step.

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

Open <http://localhost:5173>. The page loads public contacts from the backend and searches by name as you type, without refreshing the page. It shows **No contacts yet** until a contact is created in a later step. If the backend is unavailable, start it and use **Try again**.

The header navigates to `/login` and `/signup`, which remain placeholder pages until step 5A.2 connects them to the working backend. `/admin/activity` is also a placeholder route; it shows no private data and is not linked from the public header. The backend already restricts `/api/admin/**` to `ADMIN`, but the activity endpoint is not implemented yet.

`npm ci` is needed after cloning or changing dependencies, not before every run. Stop each application with Ctrl+C in its terminal.

## Ports and proxy

| Application | Default port | Configuration |
| --- | --- | --- |
| contacts-api | 8080 | `backend/src/main/resources/application.properties` |
| activity-service | 8081 | `microservice/src/main/resources/application.properties` |
| React / Vite | 5173 | `frontend/vite.config.ts` |

The browser requests `/api/contacts` from Vite on port 5173. Vite forwards `/api/*` to the main API on port 8080. The applications run in separate processes, while the browser uses one origin. The future account forms can use the same origin for their session cookie and CSRF requests.

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

The backend tests use Testcontainers with PostgreSQL 17.11 on a dynamically assigned port. They need Docker, but do not require the root `.env`, the `local` profile or the development database. They apply V1 and V2 to an empty database, verify SQL constraints, public contact reads, signup validation, BCrypt hashing, session login/logout, CSRF protection and the admin role rule. Test data stays in temporary PostgreSQL containers, which are removed after the test process ends.

With all apps running, check:

- Both Java health endpoints return HTTP 200 with `{"status":"UP"}`.
- <http://localhost:5173/api/health> returns the same response through Vite.
- `GET http://localhost:8080/api/contacts` returns HTTP 200 and `[]` before any contacts are created.
- The page shows **No contacts yet** with an empty database. Searching for a name shows **No matching contacts**, and **Clear search** returns to the full list without a page reload.
- The header links open `/login` and `/signup` without a full page reload. Opening `/admin/activity` directly shows only the placeholder.
- If you stop the main API and reload the page, it offers **Try again**; after restarting the API, that button loads the list.

## Next milestone

Step 5 is split into checkpoints. **5A.1** implements and verifies the signup, login, logout, session and admin-role backend first; its code is explained before frontend work starts. **5A.2** then replaces the React account placeholders with working forms. **5B** adds protected contact creation, editing and deletion. Photo paths are temporarily nullable until the upload feature is implemented. See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the full plan.

For the first learning checkpoint in 5A.1, use the [Romanian database and signup guide](output/pdf/pasul-1-baza-de-date-si-inregistrare.pdf). It follows the actual code at commit `44618e2`, includes a visual request flow and DBeaver exercise, and is meant to be studied in short sessions. The five learning checkpoints are recorded in [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md).
