# Setup Guide

This guide is for running the current Mini Library stack on a fresh machine or switching cleanly between Docker and local development.

## 1. What You Need

Fastest path:

- Docker Desktop

Local development path:

- Docker Desktop
- JDK 25
- Node.js 24+

Optional:

- Git
- IntelliJ IDEA or another JavaScript/Java IDE

## 2. Fastest Path: Full Docker Stack

From the project root:

```cmd
copy compose.env.example .env
notepad .env
docker compose up -d --build
```

Important:

- `.env` is now the local source of Docker/runtime secrets
- `compose.env.example` is only a sample file
- do not commit `.env`

If you need a clean database reset:

```cmd
docker compose down -v
docker compose up -d --build
```

Open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

## 3. Seeded Accounts

Seeded realm users:

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

Seeded local-account notes:

- matching `app_user` rows are also seeded by Flyway
- seeded demo users now ship with deterministic Keycloak subject ids, and migration `V19__stabilize_demo_keycloak_ids.sql` keeps existing local rows aligned
- automatic username relinking is limited to legacy `seed-*` demo identities only
- a brand-new local row is auto-created only for identities that resolve to `MEMBER`
- auto-created members default to `ACTIVE` + `GOOD_STANDING` with no branch assignment
- staff and admin identities still require local provisioning

Self-registration note:

- the shipped Keycloak realm enables registration and password reset
- it does not automatically grant a usable library role
- a fresh self-registered Keycloak account is therefore not a ready-to-use library application account

Branch mapping:

- `CENTRAL`: `branch.librarian`, `branch.manager`, `central.member`
- `EAST`: `east.librarian`, `east.manager`, `east.member`
- `HQ`: `hq.librarian`, `hq.manager`, `hq.member`

User-control hierarchy:

- branch librarians cannot manage users directly; they can only submit manager-review requests for same-branch members
- branch managers can manage librarians and members in their own branch
- admins can manage all users globally
- auditors can review users globally in read-only mode

Member/account status coverage in seeded data:

| Username | Password | Account status | Membership status | Notes |
| --- | --- | --- | --- | --- |
| `reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member account |
| `alina.reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member account |
| `central.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `CENTRAL` |
| `east.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `EAST` |
| `hq.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `HQ` |
| `hoang.nguyen` | `reader123` | `ACTIVE` | `OVERDUE_RESTRICTED` | Can log in, but borrow/reserve creation is blocked |
| `maya.tran` | `reader123` | `ACTIVE` | `BORROW_BLOCKED` | Can log in, but borrow/reserve creation is blocked |

Implemented but not seeded with demo logins:

- account status: `PENDING_VERIFICATION`, `SUSPENDED`, `LOCKED`, `ARCHIVED`
- membership status: `EXPIRED`

Discipline transitions:

- `SUSPEND` -> `SUSPENDED`
- `BAN` -> `LOCKED`
- `REINSTATE` -> `ACTIVE`

Keycloak admin console:

- `admin / admin`

## 4. Supported Local Development Modes

### 4.1 Local backend with Docker infrastructure

Start supporting services:

```bash
docker compose up -d postgres keycloak otel-collector
```

If the Docker backend is running, stop it first:

```bash
docker compose stop backend
```

Run the backend locally:

```powershell
./mvnw spring-boot:run
```

### 4.2 Local frontend with local or Docker backend

```bash
cd frontend
npm install
npm run dev
```

Local Vite frontend runs on `http://localhost:5173` by default.

### 4.3 Docker frontend with local backend

If you want the containerized frontend on `http://localhost:3000` while the backend runs locally on `8080`:

```bash
docker compose up -d frontend
```

## 5. Port Collision Rules

This project can run either the Docker backend or the local Spring Boot backend on `8080`, but not both at the same time.

To switch from Docker backend to local backend:

```bash
docker compose stop backend
```

Then run:

```powershell
./mvnw spring-boot:run
```

To switch from local backend to Docker backend:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique
Stop-Process -Id <PID>
docker compose up -d backend frontend
```

Similarly, only one frontend should own `3000`.

## 6. Main Pages

- `/`
  - discovery landing page
- `/books`
  - books workspace with search, category filter, tag filter, and cover images
- `/upcoming`
  - dedicated upcoming acquisitions workspace
- `/books/:id`
  - book detail page
- `/me`
  - member self-service page
- `/admin`
  - operations workspace, visible according to current permissions

## 7. Useful Commands

Run backend tests:

```cmd
mvnw.cmd test
```

Build frontend:

```bash
cd frontend
npm run build
```

Stop all containers:

```bash
docker compose down
```

Stop and remove volumes:

```bash
docker compose down -v
```

Restart backend and frontend containers:

```bash
docker compose up -d backend frontend
```

Verify the documented runtime URLs:

```cmd
scripts\verify-runtime.cmd
```

If the frontend is running locally through Vite on `5173`:

```cmd
scripts\verify-runtime.cmd vite
```

Verify Flyway migration state:

```cmd
scripts\verify-migrations.cmd
```

Create a database backup:

```cmd
scripts\backup-db.cmd
```

Restore a database backup:

```cmd
scripts\restore-db.cmd backups\library-before-upgrade.sql
```

## 7.1 Public Test With ngrok

Use this when you want external testers to open the current stack through public HTTPS URLs.

This setup is for one tunnel only.

Use only:

- `http://localhost:3000`

Do not expose:

- `http://localhost:8080`
- `http://localhost:8081`

Command Prompt flow:

1. Start the stack:

```cmd
docker compose up -d --build
```

2. In another Command Prompt window, start ngrok:

```cmd
ngrok http http://localhost:3000
```

3. Copy the HTTPS forwarding URL and run:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-ngrok-url>
```

The script will:

- recreate `keycloak`, `backend`, and `frontend` with the public URLs
- point backend JWT issuer validation at the public Keycloak URL under `/auth`
- rebuild the frontend to use same-origin `/api` and `/auth` routing
- update Keycloak client `library-web` with the public frontend redirect URI and web origin
- expose the full public stack through one ngrok origin instead of separate frontend/backend/Keycloak URLs

After it finishes, share the single ngrok URL with testers.

Full Windows Command Prompt how-to: [docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md](docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md)

## 8. Database Access

PostgreSQL runs on:

- Host: `localhost`
- Port: `5432`
- Database: `library`
- Username: `library_app`
- Password: `library_app_pw`

Docker shell access:

```bash
docker exec -it mini-library-postgres psql -U library_app -d library
```

Useful tables:

```sql
\dt
select * from app_user;
select * from book;
select * from book_tag;
select * from borrow_transaction;
select * from reservation;
select * from fine_record;
select * from activity_log;
```

Operational guidance for backups, restores, and secrets: [docs/OPERATIONS_RUNBOOK.md](docs/OPERATIONS_RUNBOOK.md)

## 9. Troubleshooting

### Backend is not reachable

Check:

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### `localhost:3000` refuses to connect

Usually this means the frontend container or local dev server is not running.

Check:

```bash
docker ps
```

Or, for local frontend:

```bash
cd frontend
npm run dev
```

### Docker backend fails to start on `8080`

Usually this means a local Java backend is still using the port.

Check:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### `spring-boot:run` fails because `8080` is in use

Usually this means `mini-library-backend` is already running.

Fix:

```bash
docker compose stop backend
```

### Keycloak theme looks stale

- hard refresh the browser
- use an incognito window
- restart Keycloak if necessary

## 10. Recommended Review Flow

1. Open `http://localhost:3000`
2. Review the discovery sections
3. Open `/books`
4. Open `/upcoming`
5. Filter by category or tag in `/books`
6. Open a book detail page
7. Sign in as `reader`
8. Borrow, reserve, or renew where allowed
9. Open `/me` and review activity, fines, and reservations
10. Sign in as `branch.librarian`, `branch.manager`, `admin`, or `compliance.auditor`
11. Open `/admin` and inspect the role-specific operations panels
