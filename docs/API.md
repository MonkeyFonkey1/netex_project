# HTTP contract

This document separates working endpoints from planned endpoints. The planned API describes the agreed direction; it does not mean the features already exist.

## Implemented endpoints (steps 1–4 and 5A.1 backend)

| Application | Method and path | Success response |
| --- | --- | --- |
| contacts-api (8080) | `GET /api/health` | HTTP 200, `{"status":"UP"}` |
| activity-service (8081) | `GET /internal/health` | HTTP 200, `{"status":"UP"}` |
| contacts-api (8080) | `GET /api/contacts` | HTTP 200, JSON array of public contacts |
| contacts-api (8080) | `GET /api/contacts?name=...` | HTTP 200, filtered JSON array |
| contacts-api (8080) | `GET /api/contacts/{id}` | HTTP 200, one contact; HTTP 404 if absent |
| contacts-api (8080) | `GET /api/auth/csrf` | HTTP 200, CSRF header name and token; creates an anonymous session |
| contacts-api (8080) | `POST /api/auth/signup` | HTTP 201, regular user; HTTP 400 invalid data; HTTP 409 duplicate email |
| contacts-api (8080) | `POST /api/auth/login` | HTTP 204, empty body and session cookie; HTTP 401 invalid credentials |
| contacts-api (8080) | `GET /api/auth/me` | HTTP 200, current user; HTTP 401 without login |
| contacts-api (8080) | `POST /api/auth/logout` | HTTP 204, session invalidated if present |

The two health paths are Spring Boot Actuator endpoints. Only health is exposed and component details are hidden. Since step 2, the main API's health includes a database connectivity check: a database failure can produce HTTP 503 with `{"status":"DOWN"}`. Flyway applies the SQL schema during startup. The activity service's health still only covers its standalone application, with no SQL or Kafka integration.

The frontend calls the main API via Vite's `/api` proxy. The frontend does not call the activity service directly.

The contact read endpoints require no login. The optional `name` parameter is trimmed and matches a case-insensitive substring; an absent or blank value lists all contacts. `%` and `_` are treated as ordinary characters. Results are ordered by ascending contact ID, and an empty address book returns `[]`. A search term longer than 255 characters returns HTTP 400. A public contact currently contains `id`, `name` and `address`:

```json
{"id":1,"name":"Maria Popescu","address":"Strada Exemplu 10"}
```

This is an illustrative response; the local database initially has no contacts. Internal columns such as `created_by_user_id` and `picture_path` are not returned. The public photo URL will be added with the upload feature. The backend tests insert temporary contacts into an isolated PostgreSQL container and roll those rows back.

## Planned contacts API

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /api/contacts` | Signed in | Create a contact; server assigns its author |
| `PUT /api/contacts/{id}` | Author only | Update a contact and optionally replace its photo |
| `DELETE /api/contacts/{id}` | Author only | Delete a contact |
| `GET /api/contacts/export?name=...` | Public | Download CSV, using the same optional name filter |
| `GET /api/contacts/{id}/picture` | Public | Retrieve the uploaded photograph |

Creation uses `multipart/form-data` with `name`, `address` and `picture`. Update uses the same fields; an omitted picture keeps the current photograph. Ownership comes from the authenticated session, never from a user ID supplied by the browser.

CSV contains the name, address and picture URL. Its generator must handle delimiters, quotes and line breaks correctly.

## Implemented authentication API (backend only)

Signup accepts JSON:

```json
{"email":"ana@example.com","password":"a-long-password"}
```

Login accepts `application/x-www-form-urlencoded` with form fields `email` and `password`. Spring Security processes these fields and returns 204 on success, without a response body. Call `GET /api/auth/me` to obtain the signed-in account. The React frontend will submit this form asynchronously with `fetch`.

The email is trimmed and converted to lowercase. Passwords must contain 8–72 characters and at most 72 UTF-8 bytes, because BCrypt uses at most 72 bytes. The server stores a BCrypt hash, not the original password. Signup does **not** log the user in automatically. Successful signup and `/me` responses contain only `id`, `email` and `role`, for example `{"id":1,"email":"ana@example.com","role":"USER"}`. A submitted `role` property cannot create an admin: signup always writes `USER`. Duplicate email comparison is case insensitive.

The browser first calls `GET /api/auth/csrf` and keeps the session cookie. Its response has `headerName` (`X-CSRF-TOKEN`) and `token`. The browser sends that header on **every** POST, PUT, PATCH or DELETE request, including signup, login and logout. A missing or stale token returns 403. After a successful login, call `GET /api/auth/csrf` again because login rotates the session ID and replaces the token. After logout, fetch another token before the next signup or login. The session cookie is HTTP only; the token comes from the JSON endpoint. Browser requests use the `/api` proxy so the cookie stays on one origin.

The optional `ADMIN_EMAIL` and `ADMIN_PASSWORD` server settings create an `ADMIN` account at startup if it does not exist. Both must be set together. An existing regular user with that email causes a startup error rather than an automatic promotion. A previously created admin keeps its stored password on later starts; changing the variable does not reset that password. The future admin activity API is protected by `hasRole("ADMIN")` already, but the controller and activity data are not implemented yet. The signup Kafka event belongs to a later step; signup currently saves only to SQL.

## Planned admin activity API

| Method and path | Access | Purpose |
| --- | --- | --- |
| `GET /api/admin/activities` | Admin only | List processed signup and contact activity for the admin page |

The browser will call the main API. The main API will authorize the session and obtain the activity history from `activity-service` over HTTP. The current `/admin/activity` frontend route is only a placeholder and does not call this API.

## Planned activity service

`POST /internal/activities` will accept contact activity from the main API over HTTP. A Kafka consumer in the same service will process signup events. The service will persist the processed events and provide an internal read endpoint for the main API's admin activity route.

Only the main API should use this business endpoint. Storage, delivery failure behavior and the internal access configuration must be defined when the activity feature is implemented. The current health endpoint is not the business interaction required by the assignment.

## Planned error behavior

- 400 for invalid input.
- 401 for operations that require a signed-in user.
- 403 when a signed-in user does not own the contact, or CSRF validation fails.
- 404 for a missing resource.
- 409 for a duplicate account email.
- 413 for an oversized upload.

The public contact read and backend authentication responses are implemented and tested. Contact write, export, image upload, Kafka delivery and the admin activity response remain planned.
