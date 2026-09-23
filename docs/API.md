# HTTP contract

This document separates working endpoints from planned endpoints. The planned API describes the agreed direction; it does not mean the features already exist.

## Implemented endpoints (steps 1–3)

| Application | Method and path | Success response |
| --- | --- | --- |
| contacts-api (8080) | `GET /api/health` | HTTP 200, `{"status":"UP"}` |
| activity-service (8081) | `GET /internal/health` | HTTP 200, `{"status":"UP"}` |
| contacts-api (8080) | `GET /api/contacts` | HTTP 200, JSON array of public contacts |
| contacts-api (8080) | `GET /api/contacts?name=...` | HTTP 200, filtered JSON array |
| contacts-api (8080) | `GET /api/contacts/{id}` | HTTP 200, one contact; HTTP 404 if absent |

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

## Planned authentication API

| Method and path | Purpose |
| --- | --- |
| `POST /api/auth/signup` | Register with email and password; publish a signup event |
| `POST /api/auth/login` | Authenticate and establish the session |
| `POST /api/auth/logout` | Invalidate the session |
| `GET /api/auth/me` | Retrieve the current user's identity |
| `GET /api/auth/csrf` | Obtain a CSRF token for subsequent state-changing requests |

Passwords will be hashed by Spring Security. Mutating requests will use CSRF protection. Exact authentication payloads will be finalized alongside Spring Security in step 5.

## Planned activity service

`POST /internal/activities` will accept contact activity from the main API over HTTP. A Kafka consumer in the same service will process signup events.

Only the main API should use this business endpoint. Storage, delivery failure behavior and the internal access configuration must be defined when the activity feature is implemented. The current health endpoint is not the business interaction required by the assignment.

## Planned error behavior

- 400 for invalid input.
- 401 for operations that require a signed-in user.
- 403 when a signed-in user does not own the contact, or CSRF validation fails.
- 404 for a missing resource.
- 409 for a duplicate account email.
- 413 for an oversized upload.

Only the public contact read endpoints are implemented. The 400 response for an overlong search and the 404 response for a missing contact are verified; the authentication, write, export and upload responses remain planned.
