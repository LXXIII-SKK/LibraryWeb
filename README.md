# Mini Library

Mini Library is an enterprise-style library management system built with Java 25, Spring Boot 4, PostgreSQL 18, React 19, TypeScript, Keycloak, and Docker Compose.

## Current Feature Set

- Public discovery landing page with recommendations, most borrowed titles, and most viewed titles
- Dedicated books workspace with search, category filtering, tag filtering, cover images, upcoming books, and book detail pages with availability by location
- Member self-service for borrowing, returning, renewing, reserving, fines, personal activity history, and gated online access for digital borrowings
- Operations workspace redesigned as a sidebar-driven task system with a dashboard, notifications, catalog, inventory, upcoming books, locations, access control, branches, and policy management
- Structured user discipline workflow for suspension, ban, and reinstatement with recorded reasons and history
- Enterprise access model using roles, statuses, and scope
- First-class branch centers backed by the `library_branch` table instead of raw branch IDs only
- Branch locations and holdings for physical and digital inventory
- Staff notification center for librarians, branch managers, and admins
- Hybrid catalog support with physical holdings, digital holdings, and upcoming acquisitions
- Seeded demo accounts for `MEMBER`, `LIBRARIAN`, `BRANCH_MANAGER`, `ADMIN`, and `AUDITOR`

## Runtime URLs

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

## Main Web Pages

- `/` discovery landing page
- `/books` books workspace
- `/books/:id` book detail page
- `/me` member self-service page
- `/admin` operations workspace for staff, managers, admins, and auditors according to permission set

## Enterprise Access Model

Implemented application roles:

- `MEMBER`
- `LIBRARIAN`
- `BRANCH_MANAGER`
- `ADMIN`
- `AUDITOR`

Implemented status fields:

- `account_status`
- `membership_status`

Implemented scope model:

- `SELF`
- `BRANCH`
- `GLOBAL`

Branch user-control hierarchy:

- `LIBRARIAN` cannot directly manage user accounts; librarians can only manage books/inventory and send manager-review requests for member discipline
- `BRANCH_MANAGER` can read and manage `MEMBER` and `LIBRARIAN` accounts in the same branch
- `ADMIN` can read and manage all user accounts globally
- branch-scoped staff cannot manage or view peer `BRANCH_MANAGER` accounts through the access workspace

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

## Member Status Reference

The current repo seeds several member examples, but not every possible status combination.

Seeded member login examples:

| Username | Password | Account status | Membership status | What this demonstrates |
| --- | --- | --- | --- | --- |
| `reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Normal member flow. Can log in, borrow, renew, reserve, return, and use self-service pages. |
| `alina.reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Same as `reader`, useful as a second normal member for testing reservations and circulation history. |
| `central.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Normal member at `CENTRAL`. |
| `east.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Normal member at `EAST`. |
| `hq.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Normal member at `HQ`. |
| `hoang.nguyen` | `reader123` | `ACTIVE` | `OVERDUE_RESTRICTED` | Can still log in and view self-service data, but cannot start new borrowing or reservation flows because membership is not in good standing. |
| `maya.tran` | `reader123` | `ACTIVE` | `BORROW_BLOCKED` | Can still log in and inspect account data, but self-service borrowing and reservation creation are blocked. |

Implemented account statuses without seeded login examples in the current dataset:

- `PENDING_VERIFICATION`
- `SUSPENDED`
- `LOCKED`
- `ARCHIVED`

Implemented membership statuses without seeded login examples in the current dataset:

- `EXPIRED`

Behavior model:

- `ACTIVE` account is the only account status treated as active by the app.
- non-`ACTIVE` accounts can still exist in local data, but the current seeded dataset does not include demo logins for them.
- only `GOOD_STANDING` membership allows self-service borrowing, renewal, and reservation creation.
- `OVERDUE_RESTRICTED`, `BORROW_BLOCKED`, and `EXPIRED` block new borrowing-style actions even if the user can still sign in and view profile data.

Branch coverage in the seeded dataset:

- `CENTRAL`: `branch.librarian`, `branch.manager`, `central.member`
- `EAST`: `east.librarian`, `east.manager`, `east.member`
- `HQ`: `hq.librarian`, `hq.manager`, `hq.member`

Keycloak admin console:

- `admin / admin`

## Demo Data

After a clean startup, Flyway seeds:

- a tagged multi-book catalog
- cover image support for books
- physical holdings mapped to real branch shelf locations
- several digital-only online books
- upcoming acquisitions
- staff notifications
- discovery rankings
- sample borrowings, reservations, and fines
- library policy data
- enterprise access metadata for seeded users

## Quick Start

From the project root:

```bash
docker compose up -d --build
```

If you need a clean reset:

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

Important: do not run the Docker backend and `spring-boot:run` on `8080` at the same time. Stop one before starting the other.

## Documentation

- Project documentation: [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)
- Setup guide: [SETUP_GUIDE.md](SETUP_GUIDE.md)
- Re-analysis backlog: [docs/PROJECT_REANALYSIS_BACKLOG.md](docs/PROJECT_REANALYSIS_BACKLOG.md)
- GitHub publish guide: [docs/GITHUB_PUBLISH_GUIDE.md](docs/GITHUB_PUBLISH_GUIDE.md)
- Quick operational help: [HELP.md](HELP.md)

## Notes

- The frontend is the main UI entry point. Use `http://localhost:3000`.
- The custom Keycloak theme is under `infra/keycloak/themes/library`.
- Branches are now stored as first-class library centers and surfaced in the profile, catalog, and operations UI.
- Inventory is now managed as holdings by branch and location. A full barcode/copy-transfer workflow is still a future expansion.
