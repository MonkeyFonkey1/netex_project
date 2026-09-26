# HTTP contract

This document separates working endpoints from planned endpoints. The planned API describes the agreed direction; it does not mean the features already exist.

## Implemented endpoints

| Application | Method and path | Success response |
| --- | --- | --- |
| contacts-api (8080) | `GET /api/health` | HTTP 200, `{"status":"UP"}` |
| activity-service (8081) | `GET /internal/health` | HTTP 200, `{"status":"UP"}` |
| contacts-api (8080) | `GET /api/contacts` | HTTP 200, JSON array of public contacts |
| contacts-api (8080) | `GET /api/contacts?name=...` | HTTP 200, filtered JSON array |
| contacts-api (8080) | `GET /api/contacts/export?name=...` | HTTP 200, downloadable UTF-8 CSV; public |
| contacts-api (8080) | `GET /api/contacts/{id}` | HTTP 200, one contact; HTTP 404 if absent |
| contacts-api (8080) | `POST /api/contacts` | HTTP 201, created contact and `Location` header; login required |
| contacts-api (8080) | `PUT /api/contacts/{id}` | HTTP 200, updated contact; author or ADMIN |
| contacts-api (8080) | `DELETE /api/contacts/{id}` | HTTP 204, empty body; author or ADMIN |
| contacts-api (8080) | `GET /api/contacts/{id}/picture` | HTTP 200, JPEG/PNG bytes; public; HTTP 404 if absent |
| contacts-api (8080) | `PUT /api/contacts/{id}/picture` | HTTP 200, updated contact; author or ADMIN |
| contacts-api (8080) | `DELETE /api/contacts/{id}/picture` | HTTP 204, empty body; author or ADMIN |
| contacts-api (8080) | `POST /api/auth/signup` | HTTP 201, regular user; HTTP 400 invalid data; HTTP 409 duplicate email |
| contacts-api (8080) | `POST /api/auth/login` | HTTP 204, empty body and session cookie; HTTP 401 invalid credentials |
| contacts-api (8080) | `GET /api/auth/csrf` | HTTP 200, CSRF token and header name; public |
| contacts-api (8080) | `GET /api/auth/me` | HTTP 200, current user; HTTP 401 without login |
| contacts-api (8080) | `POST /api/auth/logout` | HTTP 204, session invalidated if present |
| activity-service (8081) | `POST /internal/contact-events` | HTTP 204, contact event stored; HTTP 400 invalid data |
| activity-service (8081) | `GET /internal/activity` | HTTP 200, recent signup and contact activity for the main API |
| contacts-api (8080) | `GET /api/admin/activities` | HTTP 200, recent activity; ADMIN only; HTTP 401/403/503 otherwise |

The two health paths are Spring Boot Actuator endpoints. Only health is exposed and component details are hidden. Since step 2, the main API's health includes a database connectivity check: a database failure can produce HTTP 503 with `{"status":"DOWN"}`. Flyway applies the SQL schema during startup. The activity service's health still only covers its standalone application, with no SQL or Kafka integration.

The frontend calls the main API via Vite's `/api` proxy. The frontend does not call the activity service directly.

The contact read endpoints require no login. The optional `name` parameter is trimmed and matches a case-insensitive substring; an absent or blank value lists all contacts. `%` and `_` are treated as ordinary characters. Results are ordered by ascending contact ID, and an empty address book returns `[]`. A search term longer than 255 characters returns HTTP 400. A contact response contains `id`, `name`, `address`, `canManage` and `pictureUrl`:

```json
{"id":1,"name":"Maria Popescu","address":"Strada Exemplu 10","canManage":false,"pictureUrl":null}
```

This is an illustrative response; the local database initially has no contacts. `canManage` is true for the author or an ADMIN; the server checks the same rule for every write. `pictureUrl` is null without a photo, otherwise it is a public `/api/contacts/{id}/picture?v=...` URL. The version changes on an update. Internal columns such as `created_by_user_id` and `picture_path` are not returned. The backend tests use an isolated PostgreSQL container.

## Implemented contact writes

For now, creation and editing accept `application/json` with `name` and `address`:

```json
{"name":"Maria Popescu","address":"Strada Exemplu 10"}
```

Both values are required and trimmed on the server. Maximum lengths are 255 characters for `name` and 1000 for `address`. A missing or invalid value returns 400. The author ID always comes from the authenticated session, never from a browser-supplied field. An unsigned visitor gets 401 for writes; a signed-in USER who is not the author gets 403 for edit/delete; ADMIN can edit/delete any contact. A missing contact gets 404. `POST` returns the contact plus a `Location: /api/contacts/{id}` header. `PUT` updates the name and address without changing an existing picture path. React sends these requests asynchronously, then refreshes the contact list.

## Contact photographs

The add/edit form accepts a local JPEG or PNG file. It first saves `name` and `address` using the JSON endpoint above, then sends `PUT /api/contacts/{id}/picture` with `multipart/form-data` and one field named `picture`. For an existing contact, the same request replaces its photo. `DELETE /api/contacts/{id}/picture` removes just the photo; deleting the contact also removes its file. The author or an ADMIN can make these changes; `GET /api/contacts/{id}/picture` is public. The frontend shows a clear partial-success message if text was saved but the subsequent photo request failed, so the user can edit the contact to retry.

The server accepts files up to 5 MiB and at most 25 million pixels. It checks the actual image format, rather than trusting the filename or browser MIME type. Invalid data returns 400; a file over 5 MiB returns 413; an anonymous write returns 401; a different ordinary user's write returns 403; ADMIN may change any contact's photo; an unknown contact returns 404. A concurrent change to the same photo can return 409. Spring also limits the multipart request to 7 MiB. Stored filenames are generated by the server. PostgreSQL stores only the generated filename; the image bytes live in the local `uploads/` directory or, in Docker, the persistent `picture_data` volume. The image response has `Cache-Control: no-store`.

## Contact CSV export

`GET /api/contacts/export?name=...` is public and uses the same optional, case-insensitive name filter and 255-character limit as `GET /api/contacts`. It returns `text/csv; charset=UTF-8` with `Content-Disposition: attachment; filename="contacts.csv"`. The file has columns `name,address,picture_url` in ascending contact-ID order. `picture_url` is the public, root-relative photo URL used by the JSON API, or an empty cell when there is no photo. An empty result still downloads a header-only file.

The CSV starts with a UTF-8 BOM for spreadsheet compatibility. Every nonempty field is quoted; embedded quotes are doubled, so commas and line breaks inside a name or address remain part of that field. User text that could start a spreadsheet formula is prefixed with an apostrophe in the export. Rows end with CRLF. The React **Export CSV** link downloads all contacts or the contacts matching the current search without requiring login.

## Implemented authentication API

Signup accepts JSON:

```json
{"email":"ana@example.com","password":"a-long-password"}
```

Login accepts `application/x-www-form-urlencoded` with form fields `email` and `password`. Spring Security processes these fields and returns 204 on success, without a response body. Call `GET /api/auth/me` to obtain the signed-in account. The React frontend submits this form asynchronously with `fetch`, reads `/me` after login and on page load, and calls `/api/auth/logout` when the user signs out.

The email is trimmed and converted to lowercase. Passwords must contain 8–72 characters and at most 72 UTF-8 bytes, because BCrypt uses at most 72 bytes. The server stores a BCrypt hash, not the original password. Signup does **not** log the user in automatically. Successful signup and `/me` responses contain only `id`, `email` and `role`, for example `{"id":1,"email":"ana@example.com","role":"USER"}`. A submitted `role` property cannot create an admin: signup always writes `USER`. Duplicate email comparison is case insensitive.

The browser sends signup and login requests directly. After successful login, Spring Security associates the account with an HTTP session; the browser sends its session cookie on later requests. Browser requests use the `/api` proxy so the cookie stays on one origin. For POST, PUT and DELETE, React first calls the public `GET /api/auth/csrf` endpoint and sends the returned `token` as an `X-CSRF-TOKEN` header. Spring Security compares it with the token stored in the browser's session. A request without a valid token returns 403 before the controller runs. Login and logout clear the old token; React obtains a fresh one for the next write. After a backend restart, a write that receives 401 also clears the displayed login state.

The optional `ADMIN_EMAIL` and `ADMIN_PASSWORD` server settings create an `ADMIN` account at startup if it does not exist. Both must be set together. An existing regular user with that email causes a startup error rather than an automatic promotion. A previously created admin keeps its stored password on later starts; changing the variable does not reset that password. `/api/admin/activities` is protected by `hasRole("ADMIN")` on the server.

The local profile reads these settings from the root `.env`; Compose passes them into the Docker backend. They are not sent from the browser. Login as the configured admin and call `/api/auth/me` to see `"role":"ADMIN"`. React shows the **Activity** navigation link only for an admin and guards direct access to `/admin/activity`; the server-side check remains authoritative.

Public signup saves the user and an event in `public.event_outbox` in one SQL transaction. The worker sends the JSON event to Kafka topic `user-signups` afterward. Example payload: `{"userId":1,"email":"ana@example.com","signedUpAt":"2026-09-26T10:00:00Z"}`. The message excludes the password and its hash. `activity-service` consumes it and inserts a row in `activity.signup_events`; a repeated delivery for the same `userId` leaves one row. Signup responds 201 without waiting for Kafka. If Kafka is unavailable, the pending outbox row remains and is retried later.

## Admin activity API

| Method and path | Access | Purpose |
| --- | --- | --- |
| `GET /api/admin/activities` | Admin only | Latest 100 processed signups and latest 100 contact changes |

The browser calls the main API, which checks the session and role before requesting `GET /internal/activity` from `activity-service`. The response has `signups` (`userId`, `email`, `signedUpAt`, `processedAt`) and `contactChanges` (`eventId`, `contactId`, `actorUserId`, `action`, `occurredAt`, `processedAt`) arrays, newest first. An empty history returns two empty arrays. A visitor gets 401, a logged-in USER gets 403, and an unavailable activity service produces 503. The frontend page displays both lists and can refresh them without reloading the browser.

## Implemented contact activity over HTTP

After successful contact creation, editing or deletion, the backend queues an event and its worker sends `POST /internal/contact-events` to `activity-service`. Picture replacement and removal also queue `UPDATED` when the picture changes. The body is JSON:

```json
{"eventId":"311ba7ec-4545-44ed-9642-ef68793bd119","contactId":1,"actorUserId":2,"action":"CREATED","occurredAt":"2026-09-26T10:00:00Z"}
```

`action` is `CREATED`, `UPDATED` or `DELETED`. The activity service validates the fields, inserts into `activity.contact_events` and returns 204. A repeated `eventId` is ignored. There is no SQL foreign key from activity to contacts or users because contacts may be deleted and the microservice owns its own table. In Compose, the Docker backend calls `http://microservice:8081` on the internal network; local Java calls `localhost:8081`. The internal endpoint has no service authentication, and Compose does not publish the microservice port to the host.

The contact change and its outbox row are saved in one SQL transaction. The worker sends pending HTTP events and deletes an outbox row after a successful response; failures are retried after five seconds and survive backend restarts. Repeated delivery uses the same `eventId`, which the microservice ignores after the first insert. The browser never sends these events or calls the activity service. In the complete Docker setup, the microservice is reachable only within Compose. When run as a local development process, its endpoint is available on localhost:8081 without a service credential; the main API's admin endpoint remains protected.

## Planned error behavior

- 400 for invalid input.
- 401 for operations that require a signed-in user.
- 403 when a signed-in user does not own the contact or lacks a required role.
- 404 for a missing resource.
- 409 for a duplicate account email.
- 413 for an oversized upload.

The public contact reads, CSV export, authenticated contact writes, photo endpoints, backend authentication, Kafka signup flow, contact activity over HTTP and admin activity response are implemented and tested.
