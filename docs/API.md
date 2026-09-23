# HTTP contract

This document separates working endpoints from planned endpoints. The planned API describes the agreed direction; it does not mean the features already exist.

## Implemented in step 1

| Application | Method and path | Success response |
| --- | --- | --- |
| contacts-api (8080) | `GET /api/health` | HTTP 200, `{"status":"UP"}` |
| activity-service (8081) | `GET /internal/health` | HTTP 200, `{"status":"UP"}` |

These are Spring Boot Actuator endpoints. Only health is exposed and component details are hidden. At this milestone, UP means the Java application is running; it does not prove that SQL, Kafka or business operations work.

The frontend calls the main API via Vite's `/api` proxy. The frontend does not call the activity service directly.

## Planned contacts API

| Method and path | Access | Purpose |
| --- | --- | --- |
| `GET /api/contacts?name=...` | Public | List contacts; optional case-insensitive name search |
| `GET /api/contacts/{id}` | Public | Retrieve a contact |
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

The current skeleton has no login or contact endpoints; those rules are not implemented yet.
