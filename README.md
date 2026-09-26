 # Netex Address Book

A Java 25 / Spring Boot address book with a React + TypeScript frontend.

Docker Compose runs the full application: PostgreSQL, Kafka, the main API, the activity microservice and the React frontend. Flyway manages the main API's `public` tables and the activity service's `activity.signup_events` and `activity.contact_events` tables. Signup publishes a Kafka event that `activity-service` consumes. Contact changes send HTTP events to that service. A signed-in admin can view both histories through the main API and React.

## Repository layout

```text
backend/           contacts-api: Spring Boot, Maven
frontend/          React, TypeScript, Vite
frontend/src/features/  contact, auth and admin UI, grouped by feature
frontend/src/navigation/  client-side routes and shared header
microservice/      activity-service: Spring Boot, Maven
docs/API.md        implemented and planned HTTP endpoints
docs/database.md   table inventory, relationship diagram and DBeaver walkthrough
docs/database.svg  visual database diagram (public and activity schemas)
docs/database.png  image preview of the database diagram
docs/database.drawio editable database diagram for draw.io / diagrams.net
output/pdf/pasul-1-baza-de-date-si-inregistrare.pdf  learning guide for database and signup (Romanian)
output/pdf/pasul-2-loginul-spring-security.pdf  learning guide for login (Romanian)
compose.yaml       complete application and persistent database, Kafka and photo volumes
PROJECT_CONTEXT.md development decisions and progress (Romanian)
```

Each Java application has its own `pom.xml` and Maven Wrapper. Docker builds and starts them together; for local development, each can also run from its own directory.

In the main backend, `auth/AppUser.java` is both the user read from SQL and the Spring Security `UserDetails` kept in the session. `contact/ContactReadController.java` handles the public list and single-contact endpoints; `contact/ContactExportController.java` handles the CSV download, and `ContactCsvService.java` formats its contents. `contact/ContactWriteController.java` handles POST, PUT and DELETE. The controllers delegate to `ContactService`, which applies contact rules, and `ContactRepository`, which runs SQL. The security configuration requires login for writes; the service permits the author or an admin to edit and delete.

## Run the complete application with Docker

You need Docker Desktop/Engine with Docker Compose and an internet connection for the first build. Java and Node.js do **not** need to be installed on the machine running the complete Docker stack.

Clone the repository, open a terminal in its root directory, and create a local environment file:

```powershell
git clone https://github.com/MonkeyFonkey1/netex_project.git
cd netex_project
Copy-Item .env.example .env
```

On macOS/Linux, use `cp .env.example .env` instead of `Copy-Item`. Skip the copy if `.env` already exists. Edit `.env` and replace `POSTGRES_PASSWORD=replace-with-your-local-password` with a password of your choice. To try the admin page, also set both `ADMIN_EMAIL` and `ADMIN_PASSWORD` (8–72 UTF-8 bytes). The backend creates that admin on its first start with this email. A later password change in `.env` does not reset an existing admin account. `.env` is ignored by Git.

Start Docker Desktop, then run:

```sh
docker compose up -d --build --wait
```

On the first run, downloading the images and Maven/npm dependencies may take several minutes. `--wait` returns successfully when PostgreSQL, Kafka, both Java services and the frontend are healthy. Open **<http://localhost:3000>**. All browser requests to `/api/*` go through the frontend container to the main API, so the login cookie stays on one browser origin. The activity microservice is available only to other Compose services, not directly on a host port.

Check the running services and endpoints:

```sh
docker compose ps
```

- <http://localhost:3000/api/health> should return `{"status":"UP"}`.
- <http://localhost:3000/api/contacts> should return a JSON list; a new database returns `[]`.
- <http://localhost:3000/login> should open the login page directly, including after a browser refresh.

If a service does not become healthy, inspect `docker compose logs --tail=100 backend microservice frontend postgres kafka`. Make sure ports **3000, 8080, 5432 and 9092** are free. The Java services wait for PostgreSQL and Kafka; the frontend waits for the main API.

Stop the stack with `docker compose down`. This preserves the database, Kafka data and uploaded photos in named volumes. The next `docker compose up -d --build --wait` reuses them. Do not add `-v` to an ordinary stop: it deletes those volumes and their data. Restarting the backend invalidates in-memory login sessions; sign in again afterward.

The main API is also reachable at <http://localhost:8080/api/health> for direct checks and local frontend development. PostgreSQL is bound to `localhost:5432` for DBeaver. The Kafka development listener is bound to `localhost:9092`. The activity service's port 8081 stays inside the Compose network. If you change the database name or user in `.env`, use those values in DBeaver. Changing the PostgreSQL password in `.env` does not change the password already stored in an existing database volume.

## Requirements for local development

- JDK **25**. Confirm with `java -version`.
- Node.js **24 LTS** and npm. Confirm with `node -v` and `npm -v`.
- Internet access on the first build to download Maven and npm dependencies.

Maven **3.9.16** is downloaded by the committed wrapper; no separate Maven installation is required. Both services use Spring Boot **3.5.16**. Frontend dependency versions are recorded in `frontend/package-lock.json`.

Docker Desktop/Engine with Compose is required for PostgreSQL and Kafka. Start both before the Java applications. Backend and microservice integration tests also require Docker and create their own temporary PostgreSQL instances; they do not need a running Kafka broker. For a visual database client, use DBeaver Community. The JDK and Node.js requirements in this section apply when running the applications outside Docker.

Local photos are saved in the ignored root `uploads/` directory. The backend creates it on the first successful upload. In Docker, the backend uses the named `picture_data` volume at `/data/pictures`, so photos survive a backend container restart or recreation. PostgreSQL stores only the generated filename in `contacts.picture_path`. Keep the database and picture volumes together if you move the application to another machine. If you alternate between a local backend and a Docker backend against the same database, they use different photo directories unless you configure both to share one location.

## Start PostgreSQL and Kafka

Start Docker Desktop, then run from the repository root:

```powershell
Copy-Item .env.example .env
```

Do this only if `.env` does not already exist. Choose a local password in `.env`, then run:

```sh
docker compose up -d --wait postgres kafka
```

On macOS/Linux, use `cp .env.example .env` for the initial copy. The root `.env` is ignored by Git and is separate from the optional frontend `.env`.

Connect with DBeaver to `localhost:5432`, database `netex`, user `netex`, and the password from the root `.env`. If you customize the database or user, use those values instead. See [the visual database walkthrough](docs/database.md).

The backend applies `V1__create_users_and_contacts.sql` and then `V2__add_user_role.sql` in schema `public`. The activity service applies its own `V1__create_signup_events.sql` and `V2__create_contact_events.sql` in schema `activity`. Refresh DBeaver after starting both services to see the separate tables and Flyway histories. There are no demo users or contacts. The database username is a PostgreSQL account, separate from address book user accounts.

To create an optional admin account, set both `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the ignored root `.env` before starting the backend. The password needs 8–72 UTF-8 bytes. The backend creates the admin only if that email does not exist. It will not promote an existing regular user or reset an existing admin password. The local profile reads these values from `.env`; Compose passes them to the Docker backend too. Restart the backend after setting them. Log in at `/login`; the **Activity** link appears only for an admin and opens `/admin/activity`. Never commit real credentials.

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

Without the `local` profile (for example, when the backend runs in Docker), supply `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD` as environment variables. Defaults are localhost, 5432, netex and netex; there is no default password. `KAFKA_BOOTSTRAP_SERVERS` defaults to `localhost:9092` for local Java processes; the Docker backend receives `kafka:19092`. The standard `SPRING_DATASOURCE_URL` can override the complete JDBC URL.

Flyway applies each migration once and validates it on later starts. Add a new migration for schema changes; do not edit an already applied migration. The main API now needs an available database to start, and its health check includes database connectivity. Database details remain hidden in the public response.

### Docker and local processes together

For local development, `docker compose up -d --wait postgres kafka` starts just those two dependencies. Run both Java applications and Vite in their own terminals using the commands below. Stop a local process on port 8080 before running the Docker backend, or a local Vite server on port 5173 before starting another Vite server. The complete Docker frontend uses port 3000, so it can coexist with local Vite on 5173.

To verify photo persistence in the complete Docker stack, add a contact with a picture, run `docker compose restart backend`, then reload <http://localhost:3000>. The photo should still appear publicly because the `picture_data` volume persists. Sign in again before editing: login sessions are held in the backend container's memory.

### Try the authentication backend

These commands use PowerShell and the backend on port 8080. Each request reuses the same `WebRequestSession`, which holds the `JSESSIONID` cookie. Choose an email you have not registered before:

```powershell
$browserSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$body = @{ email = 'ana@example.com'; password = 'a-long-password' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/auth/signup -Method Post -WebSession $browserSession -ContentType 'application/json' -Body $body
$loginForm = @{ email = 'ana@example.com'; password = 'a-long-password' }
(Invoke-WebRequest http://localhost:8080/api/auth/login -Method Post -WebSession $browserSession -ContentType 'application/x-www-form-urlencoded' -Body $loginForm).StatusCode
Invoke-RestMethod http://localhost:8080/api/auth/me -WebSession $browserSession
Invoke-RestMethod http://localhost:8080/api/auth/logout -Method Post -WebSession $browserSession
```

Signup creates only a `USER` and does not log in automatically. After saving the account, the backend publishes a JSON message to Kafka topic `user-signups` with `userId`, `email` and `signedUpAt`; it never sends the password or hash. Login is handled by Spring Security, accepts form fields and returns HTTP 204 with no body; use `/api/auth/me` to read the account. The backend hashes passwords using BCrypt and returns only `id`, `email` and `role` from signup and `/me`. See [the API contract](docs/API.md) for validation and status codes.

For this learning-focused interview baseline, CSRF protection is disabled. This keeps signup, login and logout requests free of a separate token exchange, but cookie-based authenticated write requests can be exposed to cross-site request forgery. Do not treat this configuration as production-ready. Revisit CSRF protection on a separate branch after the required application works end to end.

### Terminal 2: activity service

PowerShell:

```powershell
cd microservice
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

macOS / Linux:

```sh
cd microservice
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Health endpoint: <http://localhost:8081/internal/health>

The local profile reads the root `.env`, just like the main API. Flyway creates the separate `activity` schema, `signup_events` and `contact_events` tables. The Kafka consumer records new registrations in `signup_events`; the HTTP endpoint records contact changes in `contact_events`. The internal `GET /internal/activity` returns the latest 100 rows of each type to the main API's admin endpoint. The React page shows these rows after admin login, with a **Refresh** button. You can also inspect them directly in DBeaver under **Schemas → activity → Tables**. If `activity` is not visible, refresh the connection or enable it in DBeaver's schema selection.

This initial Kafka delivery is **best effort**. If the broker is unavailable during signup, the user account still exists and the API logs the delivery failure; there is no durable retry queue yet. A transactional outbox is a possible later reliability improvement, outside this small interview baseline.

Contact activity delivery is also best effort. The main API sends `POST /internal/contact-events` after the contact database change succeeds. If the activity service is unavailable, the contact change remains successful and the API logs the failure; the event is not retried. A picture upload or removal records an `UPDATED` event when it changes the contact. The frontend never calls the activity service directly.

### Terminal 3: frontend

```sh
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. The page loads public contacts from the backend and searches by name as you type, without refreshing the page. It shows **No contacts yet** until a signed-in user creates one. If the backend is unavailable, start it and use **Try again**.

The header navigates to `/login` and `/signup`. Registration creates a regular account and then offers a link to sign in; it does not log the user in. Login submits asynchronously, then reads `/api/auth/me` to show the signed-in email in the header. The header also signs out. Refreshing the page checks the existing session through `/me`. After login, **Add contact** opens a form for the name, address and optional JPEG/PNG photo (up to 5 MiB). Both USER and ADMIN can add contacts on the **Contacts** page. The author and any admin see **Edit** and **Delete** on a contact, including its photo controls. A visitor can read, search and use **Export CSV** without login. The export link downloads all contacts or the contacts matching the current search. Another ordinary user can create their own contact but cannot edit or delete yours. Search and form actions use asynchronous HTTP requests; the export uses a normal browser download. An admin also sees an **Activity** link to `/admin/activity`; a regular user does not. Direct navigation by a regular user shows an access message, and the backend returns 403 for `/api/admin/activities` regardless of the frontend.

`npm ci` is needed after cloning or changing dependencies, not before every run. Stop each application with Ctrl+C in its terminal.

## Ports and proxy

| Application | Default port | Configuration |
| --- | --- | --- |
| contacts-api | 8080 | `backend/src/main/resources/application.properties` |
| activity-service | 8081 inside Compose; 8081 locally | `microservice/src/main/resources/application.properties`; no Compose host port |
| PostgreSQL | 5432 | `compose.yaml` |
| Kafka (local Java apps) | 9092 | `compose.yaml`; Docker backend uses `kafka:19092` internally |
| React / Vite development | 5173 | `frontend/vite.config.ts` |
| Docker frontend (Nginx) | 3000 | `compose.yaml`, `frontend/nginx.conf` |

In local development, the browser requests `/api/contacts` and `/api/auth/*` from Vite on port 5173; Vite forwards `/api/*` to the main API on port 8080. In the complete Docker stack, Nginx serves the built React files on port 3000 and forwards `/api/*` to `backend:8080` inside Compose. Both setups keep one browser origin for the session cookie. Nginx also serves `index.html` for React routes such as `/login` and `/admin/activity` after a refresh.

Vite refuses to silently switch ports when 5173 is occupied. Stop the conflicting process or change the port deliberately.

For an optional backend port override, set `SERVER_PORT` in that backend terminal. If the main API moves, copy `frontend/.env.example` to `frontend/.env`, update `API_PROXY_TARGET`, and restart Vite. No `.env` file is required with the defaults.

`npm run preview` only previews the built frontend files; use `npm run dev` for local development or the Nginx container for the complete stack.

## IntelliJ IDEA and VS Code

Open `backend/` in IntelliJ IDEA and import its Maven project. Reload Maven after dependency changes and select JDK 25 as the Project SDK and Maven runner JRE. In **Run → Edit Configurations**, select the configuration for `ContactsApiApplication` and set:

- **Program arguments:** `--spring.profiles.active=local`
- **Working directory:** the absolute path of this repository's `backend` folder

If Program arguments is hidden, enable it through **Modify options**. Start PostgreSQL and Kafka using Compose, then run the application. This works with a standard Java Application run configuration; a dedicated Spring UI is not required. The local profile reads the password from the ignored root `.env`.

Open `microservice/` as a second IntelliJ project, set the same `--spring.profiles.active=local` program argument and set its working directory to `microservice/`, then run `ActivityServiceApplication`. Open `frontend/` in VS Code and use its terminal for npm commands.

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

The Java integration tests verify the public health path, contact reads, CSV formatting and filtering, contact writes, the author-or-admin rule, photo upload/replace/remove and that internal management information is not exposed. Spring Boot Actuator supplies the health endpoints; the contact endpoints use controllers, services and a repository.

The Java tests use Testcontainers with PostgreSQL 17.11 on dynamically assigned ports. They need Docker, but do not require the root `.env`, a local profile, the development database or a running Kafka broker. The backend tests verify its V1/V2 migrations, contacts, auth, photos, CSV, Kafka signup publication, contact HTTP calls and the admin-only activity response. The microservice tests verify its V1/V2 migrations, deduplicated signup messages, contact events and history reads. Test data stays in temporary PostgreSQL containers, which are removed after the test process ends.

With the complete Docker stack at port 3000, or the separately started applications at port 5173, check:

- The main API health endpoint returns HTTP 200 with `{"status":"UP"}` through <http://localhost:3000/api/health> (Docker) or <http://localhost:5173/api/health> (local Vite). In Docker, `docker compose ps` also shows the internal activity service as healthy.
- `GET http://localhost:8080/api/contacts` returns HTTP 200 and `[]` before any contacts are created.
- The page shows **No contacts yet** with an empty database. Searching for a name shows **No matching contacts**, and **Clear search** returns to the full list without a page reload.
- The header links open `/login` and `/signup` without a full page reload. Create a new account in the browser, sign in, refresh and confirm the email remains in the header, then sign out. Log in as the configured admin: the **Activity** link appears and the page shows recent Kafka and contact events. A USER does not see the link or data.
- After creating an account, refresh `activity.signup_events` in DBeaver. The new row's `user_id` matches `public.users.id`, and `processed_at` shows when the consumer handled the message.
- After adding, editing and deleting a contact, refresh `activity.contact_events` in DBeaver. The same `contact_id` should have `CREATED`, `UPDATED` and `DELETED`; changing its photo also yields `UPDATED`.
- Sign in and add a contact. Edit its address and confirm the list updates. Sign out: the contact remains public, but the buttons to change it disappear. A different account can see it, without its Edit/Delete buttons. Sign back in as the author to delete it.
- Add a contact with a JPEG or PNG photo from your computer. Check that the photo appears for signed-out visitors. Replace it in Edit, then use **Remove current photo** and Save. To verify storage, restart the backend and reload a contact that still has a photo.
- Use **Export CSV** while signed out. Open `contacts.csv` in a spreadsheet or text editor. With a search term, the file contains only matching contacts; without one, it contains all contacts. The columns are `name`, `address` and `picture_url`. A contact without a photo has an empty last cell. Commas, quotes and line breaks in contact text are preserved.
- If you stop the main API and reload the page, it offers **Try again**; after restarting the API, that button loads the list.

## Learning and next improvements

The complete interview baseline can be started through Docker Compose as described above. See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the implementation history and the learning plan. Potential improvements after the handoff include a separate CSRF protection branch and more reliable event delivery; neither is required for the current assignment.

For the first learning checkpoint in 5A.1, use the [Romanian database and signup guide](output/pdf/pasul-1-baza-de-date-si-inregistrare.pdf). It follows the actual code at commit `44618e2`, includes a visual request flow and DBeaver exercise, and is meant to be studied in short sessions. The five learning checkpoints are recorded in [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md).

The [login guide](output/pdf/pasul-2-loginul-spring-security.pdf) documents an earlier version that used a separate `AccountPrincipal`. In the current code, `AppUser` implements `UserDetails` directly; read `AppUser.java` and `SecurityConfig.java` for the current login flow.
