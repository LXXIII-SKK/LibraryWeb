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

```bash
docker compose up -d --build
```

If you need a clean database reset:

```bash
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

Application users:

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

Branch mapping:

- `CENTRAL`: `branch.librarian`, `branch.manager`, `central.member`
- `EAST`: `east.librarian`, `east.manager`, `east.member`
- `HQ`: `hq.librarian`, `hq.manager`, `hq.member`

Branch user-control hierarchy:

- branch librarians cannot manage users directly; they can only submit manager-review requests for members in their branch
- branch managers can manage librarians and members in their own branch
- admins can manage all users globally

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
4. Filter by category or tag
5. Open a book detail page
6. Sign in as `reader`
7. Borrow, reserve, or renew where allowed
8. Open `/me` and review activity, fines, and reservations
9. Sign in as `branch.librarian`, `branch.manager`, `admin`, or `compliance.auditor`
10. Open `/admin` and inspect the role-specific operations panels
