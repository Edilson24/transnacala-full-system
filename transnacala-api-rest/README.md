# TransNacala API

FastAPI service for the TransNacala system. Mobile and web clients communicate
with this API over HTTPS/JSON; they must never connect to MySQL directly.

## Local setup

Requires Python 3.11+ and MySQL 8.0.16+. For the API migration path, create
only an empty database in MySQL Workbench:

```sql
CREATE DATABASE transnacala
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Do not run `database/mysql_schema.sql` on this database before Alembic: that
script creates the tables itself, and the initial migration would then try to
create them a second time. The SQL script is the standalone Workbench bootstrap;
Alembic is the API-managed schema path.

Then set up the API:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
Copy-Item .env.example .env
```

The local connection defaults map to `127.0.0.1:3306`, database
`transnacala` (the name created by `mysql_schema.sql`), and the PyMySQL driver.
Set `MYSQL_USER` and `MYSQL_PASSWORD` in `.env` to the credentials you use for
MySQL Workbench. Keep credentials local and do not commit `.env`. If a password
contains URL-special characters, the separate `MYSQL_*` settings handle it
without URL encoding. Alternatively, configure a complete `DATABASE_URL`,
which takes precedence. The API can start without credentials to serve
liveness and documentation; readiness returns `503` until MySQL is configured
and reachable.

Fill in `MYSQL_USER` and `MYSQL_PASSWORD` in `.env`, then apply versioned
migrations and start the API:

```powershell
python -m alembic upgrade head
python -m uvicorn main:app --reload --port 8000
```

On a newly migrated database, create the first administrator from an interactive
terminal. The password is entered without echo and must contain at least 12
characters:

```powershell
python -m app.cli create-admin admin
```

For development/testing only, create the requested sample accounts using
interactive prompts (passwords are hidden while typing). The command is
restricted to `APP_ENV=development`, skips existing usernames without changing
their passwords, and enforces the API's minimum length of 12 characters:

```powershell
python -m app.cli seed-test-users
```

It creates `edilson` (Edilson Vitorino Hilario, ADMIN), `anifa` (Anifa Armando,
PASSENGER), and `silva` (Silva Atumane, DRIVER). Set distinct passwords when
prompted; test passwords such as `edilson123456`, `anifa1234567`, and
`silva1234567` meet the current length rule, but must not be reused outside a
local test database.

Set `AUTH_SECRET_KEY` to a private random value of at least 32 bytes before
starting the API. Generate one locally with
`python -c "import secrets; print(secrets.token_urlsafe(48))"` and keep it only
in `.env` or your deployment secret store. If omitted in development, the API
uses an ephemeral process key and existing access tokens stop working after a
restart. Access tokens expire after `ACCESS_TOKEN_EXPIRE_SECONDS` (one hour by
default). Passwords use salted PBKDF2-HMAC-SHA256 hashes and are never included
in API responses.

Interactive OpenAPI documentation is at `/docs`; the machine-readable contract
is at `/openapi.json`. Process liveness and database readiness are at
`/health/live` and `/health/ready`. Schema changes must be added as Alembic
revisions; do not edit a deployed database manually.

Run the automated tests with `pytest`. Tests use SQLite in-memory and do not
require MySQL. The bootstrap SQL and Alembic migration are both maintained
against the same initial relational model.

## API contract status

`app/openapi.json` defines the reviewed HTTP contract for authentication,
accounts, catalogue, assignments, sync and tracking. Entity IDs are UUID
strings. List endpoints use `limit`/`offset`; incremental sync uses
`since_id`/`next_since_id`. Errors have the shape
`{"error":{"code":"...","message":"...","fields":[]}}`. Passwords, tokens and
database details are never returned in error bodies.

Authentication, account administration, route/stop/vehicle management,
assignments and incremental catalogue change reads are implemented. Vehicle
codes are allocated under a transaction lock on the shared sync sequence.
Catalogue writes and their sync events commit atomically. Tracking endpoints
remain reserved for a later phase and return the explicit
`501 DOMAIN_ENDPOINT_NOT_IMPLEMENTED` error until implemented.
