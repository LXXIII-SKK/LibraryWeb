# Mini Library

Mini Library is a full-stack library system with public discovery, member self-service, and a permission-scoped operations workspace. The current stack uses Java 25, Spring Boot 4, PostgreSQL 18, React 19, TypeScript, Keycloak, and Docker Compose.

## Current Feature Set

- Public discovery landing page with paged recommendations, most borrowed titles, most viewed titles, and upcoming arrivals
- Books workspace with category and tag filters, 4-card pagination, cover images, branch-aware availability, and detail pages
- Dedicated upcoming workspace with branch/search filtering and 4-card pagination
- Member self-service for borrowing, returning, renewing, reserving, collecting ready holds, fines, notifications, digital access, and personal activity history
- Operations workspace for librarians, branch managers, admins, and auditors according to the authenticated permission set
- Branches, locations, physical holdings, digital holdings, targeted staff notifications, and upcoming acquisitions
- User access management with role, account status, membership status, branch/home-branch assignment, and recorded discipline history
- Librarian discipline-review requests that notify branch managers and admins for same-branch member issues
- Keycloak-hosted login, registration, logout, password reset, and account-management pages with a custom theme

## Runtime URLs

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

## Main Web Pages

- `/`
  - discovery landing page
- `/books`
  - books workspace
- `/upcoming`
  - upcoming acquisitions workspace
- `/books/:id`
  - book detail page
- `/me`
  - member and account workspace
- `/admin`
  - permission-scoped operations workspace

## Access Model

Implemented roles:

- `MEMBER`
- `LIBRARIAN`
- `BRANCH_MANAGER`
- `ADMIN`
- `AUDITOR`

Implemented status fields:

- `account_status`
- `membership_status`

Implemented scopes:

- `SELF`
- `BRANCH`
- `GLOBAL`

### User Account Provisioning

- Flyway seeds local `app_user` rows with role, status, branch, and home-branch data.
- The imported Keycloak realm seeds matching login accounts.
- The shipped realm now uses deterministic Keycloak subject IDs for demo users, and migration `V19__stabilize_demo_keycloak_ids.sql` aligns existing local rows to those ids.
- `CurrentUserService` first matches by Keycloak subject. Automatic username relinking is now limited to legacy `seed-*` demo identities only.
- A brand-new local row is auto-created only when the authenticated principal resolves to the `MEMBER` role.
- Auto-created members default to `ACTIVE` + `GOOD_STANDING` with no branch assignment until staff update them locally.
- Staff and admin identities require local provisioning; they are not auto-created on first login.
- The shipped Keycloak realm enables registration and password reset, but it does not ship automatic `member` role assignment. A fresh self-registered Keycloak user is therefore not a ready-to-use application account without follow-up provisioning.

### User Control Hierarchy

- `LIBRARIAN`
  - cannot list or edit users directly
  - can manage catalog and inventory in-branch
  - can submit a discipline-review request for a member in the same branch
- `BRANCH_MANAGER`
  - can read and manage `MEMBER` and `LIBRARIAN` accounts in the same branch
  - cannot manage peer `BRANCH_MANAGER` accounts or any global-role accounts
  - can suspend, ban, or reinstate manageable same-branch users according to state rules
- `ADMIN`
  - can read and manage all users globally
  - can change roles, branches, statuses, and discipline state
- `AUDITOR`
  - has global read-only visibility into operational data, including users
  - cannot mutate users and does not get effective-permission visibility for other users

## Demo Accounts

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

Keycloak admin console:

- `admin / admin`

## Member Status Reference

The application uses two separate state fields:

- `account_status` controls whether the account is treated as active for protected application actions
- `membership_status` controls whether a member can start borrowing-style self-service flows

Current behavior:

- only `ACTIVE` accounts are treated as active by the application
- only `GOOD_STANDING` membership allows self-service borrow, renew, reserve, and collect actions
- `OVERDUE_RESTRICTED`, `BORROW_BLOCKED`, and `EXPIRED` still allow profile-style inspection if the user can authenticate, but they block new borrowing-style actions

Seeded member login examples:

| Username | Password | Account status | Membership status | What it demonstrates |
| --- | --- | --- | --- | --- |
| `reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member flow |
| `alina.reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Second standard member for circulation testing |
| `central.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `CENTRAL` |
| `east.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `EAST` |
| `hq.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Standard member in `HQ` |
| `hoang.nguyen` | `reader123` | `ACTIVE` | `OVERDUE_RESTRICTED` | Can inspect account state, but cannot start new borrowing or reservation flows |
| `maya.tran` | `reader123` | `ACTIVE` | `BORROW_BLOCKED` | Can inspect account state, but cannot start new borrowing or reservation flows |

Implemented without seeded login examples:

- account status: `PENDING_VERIFICATION`, `SUSPENDED`, `LOCKED`, `ARCHIVED`
- membership status: `EXPIRED`

Branch coverage in the seeded dataset:

- `CENTRAL`: `branch.librarian`, `branch.manager`, `central.member`
- `EAST`: `east.librarian`, `east.manager`, `east.member`
- `HQ`: `hq.librarian`, `hq.manager`, `hq.member`

Discipline transitions:

- `SUSPEND` -> `SUSPENDED`
- `BAN` -> `LOCKED`
- `REINSTATE` -> `ACTIVE`
- only `ACTIVE` users can be suspended or banned
- only `SUSPENDED` or `LOCKED` users can be reinstated

## Demo Data

After a clean startup, the database contains:

- a tagged multi-book catalog
- cover image support
- branch-aware locations and holdings
- digital access URLs for digital holdings
- upcoming books
- staff notifications
- discovery ranking data
- sample borrowings, reservations, and fines
- branch and user-access metadata
- discipline workflow tables and event logging support

## Quick Start

From the project root:

```bash
docker compose up -d --build
```

For a clean reset:

```bash
docker compose down -v
docker compose up -d --build
```

## Local Development

Prerequisites:

- JDK 25
- Node.js 24+
- Docker Desktop

Backend:

```powershell
./mvnw spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Important:

- do not run the Docker backend and the local JVM backend on `8080` at the same time
- `/me` exposes a Keycloak account-management link for profile and password changes; those edits are not handled by a local profile form

## Testing

Backend:

```powershell
./mvnw test
```

- includes repository-free unit/service tests, controller/security tests, and PostgreSQL-backed integration tests via Testcontainers
- requires a working Docker engine for the Testcontainers integration layer; when Docker is unavailable, those integration tests are skipped

Frontend browser smoke:

```powershell
cd frontend
npx playwright install chromium
npm run test:smoke
```

- exercises the public discovery shell, catalog-to-detail navigation, and an admin workspace smoke path
- uses a browser-level test auth shim and mocked API fixtures, so it does not require a live Keycloak login flow

## Public Test With ngrok

For Windows Command Prompt and one free tunnel only:

1. Start the Docker stack.
2. In another terminal, run `ngrok http http://localhost:3000`.
3. From the repo root, run:

```cmd
scripts\start-public-test.cmd -PublicUrl https://<your-ngrok-url>
```

Tunnel only port `3000`. Do not expose `8080` or `8081` directly. The frontend will proxy `/api` and `/auth` after the script runs.

Detailed how-to: [docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md](docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md)

## Documentation

- Project documentation: [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)
- Setup guide: [SETUP_GUIDE.md](SETUP_GUIDE.md)
- Windows single-port ngrok guide: [docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md](docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md)
- Flow documents: [docs/flows/README.md](docs/flows/README.md)
- Re-analysis backlog: [docs/PROJECT_REANALYSIS_BACKLOG.md](docs/PROJECT_REANALYSIS_BACKLOG.md)
- GitHub publish guide: [docs/GITHUB_PUBLISH_GUIDE.md](docs/GITHUB_PUBLISH_GUIDE.md)
- Quick operational help: [HELP.md](HELP.md)

## Notes

- The frontend is the main UI entry point. Use `http://localhost:3000`.
- `/upcoming` is the dedicated arrival-planning page; the home page keeps a paged upcoming rail at the end.
- `/admin` is an operations workspace, not a literal admin-only page.
- The custom Keycloak theme is under `infra/keycloak/themes/library`.
- The shipped realm supports registration, but usable new-account provisioning is still incomplete.
