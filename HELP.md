# Help

Quick reference for running the Mini Library stack and understanding the seeded account model.

## Main URLs

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

## Start The Full Stack

```bash
docker compose up -d --build
```

## Stop The Full Stack

```bash
docker compose down
```

## Clean Reset

```bash
docker compose down -v
docker compose up -d --build
```

## Run Backend Locally

Stop the Docker backend first:

```bash
docker compose stop backend
```

Then run:

```powershell
./mvnw spring-boot:run
```

## Switch Back To Docker Backend

Stop the local Java process on `8080`, then:

```bash
docker compose up -d backend frontend
```

## Run Frontend Locally

```bash
cd frontend
npm install
npm run dev
```

## Demo Accounts

Realm users:

- `admin / admin123`
- `reader / reader123`
- `alina.reader / reader123`
- `hoang.nguyen / reader123`
- `maya.tran / reader123`
- `branch.librarian / librarian123`
- `branch.manager / manager123`
- `east.librarian / librarian123`
- `east.manager / manager123`
- `hq.librarian / librarian123`
- `hq.manager / manager123`
- `central.member / reader123`
- `east.member / reader123`
- `hq.member / reader123`
- `compliance.auditor / auditor123`

Keycloak admin console:

- `admin / admin`

## Account Model Highlights

- Matching local `app_user` rows are seeded by Flyway and now line up with deterministic demo-user Keycloak subject ids.
- Automatic username relinking is limited to legacy `seed-*` demo identities only.
- Auto-created member accounts default to `ACTIVE` + `GOOD_STANDING` with no branch assignment.
- Only `ACTIVE` accounts are treated as active by the application.
- Only `GOOD_STANDING` members can start new borrowing-style self-service actions.
- The shipped realm exposes registration, but it does not automatically provision a usable library account for a brand-new signup.

## Main Pages

- `/`
  - discovery landing page with paged most-borrowed, most-viewed, and upcoming sections
- `/books`
  - catalog explorer with 4 books per page
- `/upcoming`
  - dedicated upcoming acquisitions workspace
- `/me`
  - member/account workspace
- `/admin`
  - permission-scoped operations workspace

## User Control Hierarchy

- `branch.librarian`, `east.librarian`, `hq.librarian`
  - cannot manage users directly
  - can work with catalog and inventory
  - can submit discipline-review requests for same-branch members
- `branch.manager`, `east.manager`, `hq.manager`
  - can manage members and librarians in their own branch
  - cannot manage peer branch managers or global-role accounts
- `admin`
  - can manage all users globally
- `compliance.auditor`
  - can review user data globally in read-only mode

## Member Status Login Examples

- `reader / reader123`
  - `ACTIVE` + `GOOD_STANDING`
  - normal member flow
- `alina.reader / reader123`
  - `ACTIVE` + `GOOD_STANDING`
  - second normal member for circulation testing
- `hoang.nguyen / reader123`
  - `ACTIVE` + `OVERDUE_RESTRICTED`
  - can log in, but new borrow/reserve flows are blocked
- `maya.tran / reader123`
  - `ACTIVE` + `BORROW_BLOCKED`
  - can log in, but new borrow/reserve flows are blocked
- `central.member / reader123`
  - `ACTIVE` + `GOOD_STANDING`
  - normal member at `CENTRAL`
- `east.member / reader123`
  - `ACTIVE` + `GOOD_STANDING`
  - normal member at `EAST`
- `hq.member / reader123`
  - `ACTIVE` + `GOOD_STANDING`
  - normal member at `HQ`

Implemented without seeded demo logins:

- account: `PENDING_VERIFICATION`, `SUSPENDED`, `LOCKED`, `ARCHIVED`
- membership: `EXPIRED`

Discipline transitions:

- `SUSPEND` -> `SUSPENDED`
- `BAN` -> `LOCKED`
- `REINSTATE` -> `ACTIVE`

## Common Problems

### `spring-boot:run` says port `8080` is already in use

Docker backend is probably still running:

```bash
docker compose stop backend
```

### Docker backend says port `8080` is already in use

Local Java backend is probably still running:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique
Stop-Process -Id <PID>
```

### `localhost:3000` refuses to connect

Frontend is not running:

```bash
docker compose up -d frontend
```

Or run the Vite frontend locally:

```bash
cd frontend
npm run dev
```

## Useful Checks

Backend health:

```bash
curl http://localhost:8080/actuator/health
```

Running containers:

```bash
docker ps
```

Verify frontend, backend, and Keycloak together:

```cmd
scripts\verify-runtime.cmd
```
