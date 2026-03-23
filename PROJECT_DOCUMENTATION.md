# Mini Library Management System Documentation

## 1. Overview

Mini Library is a full-stack library system with public discovery, member self-service, and enterprise-style operations tooling. The current implementation uses a role + status + scope access model and supports catalog search, tags, cover images, borrowing, renewals, reservations, fines, policy management, audit review, user access administration, branch-aware holdings, digital borrowing, staff notifications, and upcoming acquisitions.

Core technologies:

- Backend: Java 25, Spring Boot 4, Spring Security, Spring Data JPA, Spring Modulith
- Frontend: React 19, TypeScript, Vite
- Database: PostgreSQL 18
- Identity: Keycloak
- Infrastructure: Docker Compose
- Observability: Prometheus, Grafana, OpenTelemetry Collector

## 2. Access Model

### 2.1 Implemented roles

- `MEMBER`
- `LIBRARIAN`
- `BRANCH_MANAGER`
- `ADMIN`
- `AUDITOR`

### 2.2 Implemented statuses

Account status:

- `PENDING_VERIFICATION`
- `ACTIVE`
- `SUSPENDED`
- `LOCKED`
- `ARCHIVED`

Membership status:

- `GOOD_STANDING`
- `OVERDUE_RESTRICTED`
- `BORROW_BLOCKED`
- `EXPIRED`

### 2.3 Implemented scope model

- `SELF`
- `BRANCH`
- `GLOBAL`

The access layer is permission-driven. Roles expand into permissions, and services enforce self, branch, or global visibility rather than relying on route names alone.

### 2.4 Branch user-control hierarchy

User access management is intentionally hierarchical:

- `LIBRARIAN`
  - cannot read or manage user accounts directly
  - can manage catalog and inventory inside the branch
  - can submit a discipline-review request for a same-branch member through notifications
- `BRANCH_MANAGER`
  - can read and manage `MEMBER` and `LIBRARIAN` accounts in the same branch
  - cannot read or manage peer `BRANCH_MANAGER` accounts or any global role accounts
  - can see effective permission maps for manageable same-branch users
- `ADMIN`
  - can read and manage all user accounts globally
  - can change roles, statuses, branches, and discipline state
- `AUDITOR`
  - can read user records globally in a read-only mode
  - cannot mutate users and does not get other users' effective permission maps

This hierarchy applies to:

- user listing in the access workspace
- individual access detail views
- access updates
- discipline actions
- visibility of effective permission maps

### 2.5 Member state reference

The project uses two separate state fields:

- `account_status`
- `membership_status`

They are not the same thing:

- `account_status` controls whether the account is considered active by the application
- `membership_status` controls whether a member is allowed to start borrowing-style actions

Current behavior in code:

- only `ACTIVE` account status is treated as active
- only `GOOD_STANDING` membership status allows self-service borrowing, renewing, reservation creation, and reservation collection
- non-good-standing membership statuses can still sign in if Keycloak authentication succeeds, but the application blocks new borrowing-style actions

Account status meaning:

- `PENDING_VERIFICATION`
  - account exists locally but is not treated as active by the app
  - protected actions are blocked until status is changed to `ACTIVE`
  - no seeded demo login currently ships for this state
- `ACTIVE`
  - the only account status treated as active by the app
  - capabilities still depend on `membership_status`
- `SUSPENDED`
  - the account is not treated as active by the app
  - protected actions are blocked
  - no seeded demo login currently ships for this state
- `LOCKED`
  - the account is not treated as active by the app
  - protected actions are blocked
  - used as the resulting status for a `BAN` discipline action
  - no seeded demo login currently ships for this state
- `ARCHIVED`
  - the account is not treated as active by the app
  - protected actions are blocked
  - no seeded demo login currently ships for this state

Membership status meaning:

- `GOOD_STANDING`
  - member can use normal self-service borrowing, renewing, reservation creation, and reservation collection
- `OVERDUE_RESTRICTED`
  - member can sign in and inspect account data
  - self-service borrowing, renewal, and reservation creation are blocked
- `BORROW_BLOCKED`
  - member can sign in and inspect account data
  - self-service borrowing, renewal, and reservation creation are blocked
- `EXPIRED`
  - member can exist in local data, but borrowing-style self-service actions are blocked
  - no seeded demo login currently ships for this state

Seeded member login examples:

| Username | Password | Account status | Membership status | Result in app |
| --- | --- | --- | --- | --- |
| `reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Full normal member flow. |
| `alina.reader` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Full normal member flow with separate history for testing. |
| `central.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Full normal member flow in `CENTRAL`. |
| `east.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Full normal member flow in `EAST`. |
| `hq.member` | `reader123` | `ACTIVE` | `GOOD_STANDING` | Full normal member flow in `HQ`. |
| `hoang.nguyen` | `reader123` | `ACTIVE` | `OVERDUE_RESTRICTED` | Can log in and inspect account data, but cannot start new borrowing or reservation flows. |
| `maya.tran` | `reader123` | `ACTIVE` | `BORROW_BLOCKED` | Can log in and inspect account data, but cannot start new borrowing or reservation flows. |

Implemented but not currently seeded with demo login credentials:

Account status:

- `PENDING_VERIFICATION`
- `SUSPENDED`
- `LOCKED`
- `ARCHIVED`

Membership status:

- `EXPIRED`

Discipline transitions:

- `SUSPEND` -> `SUSPENDED`
- `BAN` -> `LOCKED`
- `REINSTATE` -> `ACTIVE`
- only `ACTIVE` users can be suspended or banned
- only `SUSPENDED` or `LOCKED` users can be reinstated

### 2.6 Identity provisioning and seeded account synchronization

The shipped identity model has two layers:

- imported Keycloak realm users for browser login
- local `app_user` rows for application role, branch, and status enforcement

Current behavior:

- seeded demo users now ship with deterministic Keycloak subject ids in `realm-library.json`
- migration `V19__stabilize_demo_keycloak_ids.sql` aligns existing Flyway-seeded `app_user` rows to those stable ids
- on the first authenticated request, `CurrentUserService` first looks up the JWT subject, then only falls back to username when the local row still carries a legacy `seed-*` placeholder
- a brand-new local user is auto-created only when the authenticated principal resolves to `MEMBER`
- new auto-created members default to `ACTIVE` + `GOOD_STANDING` with no branch or home-branch assignment until local provisioning changes them
- non-member identities require local provisioning before they can use the application

Shipped-realm caveat:

- registration is enabled in Keycloak, but new self-registered accounts are not automatically provisioned into ready-to-use library accounts
- seeded demo identities now survive Keycloak recreation cleanly, but a full Keycloak-to-local admin provisioning and synchronization workflow is still not implemented

## 3. High-Level Architecture

### 3.1 Runtime architecture

- React renders the browser UI
- Spring Boot exposes REST endpoints under `/api`
- PostgreSQL stores catalog, circulation, policy, and identity data
- Keycloak handles login, registration, logout, password reset, account management, and token issuance
- Docker Compose orchestrates the local stack
- optional public-test mode serves the SPA, `/api`, and Keycloak `/auth` behind one frontend origin through nginx proxying

### 3.2 Backend module structure

- `catalog`
  - books, metadata, cover images, tags, filter metadata
- `circulation`
  - borrow, return, renew, reservations, fines, policy
- `history`
  - audit logs and book view tracking
- `identity`
  - local user model, profile, permission resolution, access management
- `branch`
  - library centers, branch directory, and branch metadata
- `inventory`
  - branch locations, physical and digital holdings, digital access links
- `discovery`
  - recommendations and weekly ranking responses
- `notification`
  - targeted staff notifications and read receipts
- `upcoming`
  - acquisition planning and upcoming books
- `config`
  - security, CORS, exception handling, runtime configuration

### 3.3 Layering

Presentation layer:

- React pages and components
- REST controllers such as `BookController`, `BorrowController`, `ReservationController`, and `AccessManagementController`

Business layer:

- service classes implementing authorization and business rules
- examples: borrowing eligibility, reservation precedence, fine waiver authorization, policy enforcement

Data access layer:

- Spring Data JPA repositories
- PostgreSQL tables managed with Flyway migrations

## 4. Current Functional Scope

### 4.1 Public discovery and catalog

Anonymous users can:

- open the landing page
- browse discovery sections with paged 4-card rails
- search books
- filter by category
- filter by tags sourced across the full catalog
- browse the dedicated upcoming acquisitions page
- open book detail pages
- view public availability summaries
- see where a physical title is currently available
- see whether a title has online availability
- browse upcoming acquisitions
- view book cover images

### 4.2 Member self-service

Members can:

- view their own profile and effective permissions
- borrow books for themselves
- return their own active borrowings
- renew eligible borrowings
- create and cancel reservations
- view active reservations
- view fines and overdue context
- inspect their activity history

Borrow due dates are generated from the active library policy. Members do not submit arbitrary due dates from the UI.

Status effect summary for members:

- `ACTIVE` + `GOOD_STANDING`
  - can use normal self-service circulation actions
- `ACTIVE` + `OVERDUE_RESTRICTED`
  - can sign in and inspect account state, but self-service borrowing-style actions are blocked
- `ACTIVE` + `BORROW_BLOCKED`
  - can sign in and inspect account state, but self-service borrowing-style actions are blocked
- non-`ACTIVE` account statuses
  - the app does not treat the account as active for protected member actions
  - no seeded demo logins currently ship for these account states

### 4.3 Staff and operations workspace

The `/admin` route is an operations workspace, not a literal admin-only page. The visible panels depend on the authenticated permission set.

Implemented operations include:

- dashboard-first sidebar task workspace
- catalog create and update for authorized staff
- admin-only book deletion
- cover upload
- branch create and update
- location create and update
- holding create and update for physical and digital inventory
- upcoming book create, update, and removal
- staff notification broadcast and inbox
- operational borrowing review
- force return for authorized staff
- reservation review and no-show handling
- fine review and waivers
- user access review and update
- hierarchy-scoped suspension, ban, reinstatement, and discipline history with recorded reasons
- librarian-to-manager discipline escalation requests through the notifications workspace
- policy review and update
- activity log review

Access workspace user-control boundaries:

- librarians do not receive the access panel
- branch managers see member and librarian accounts from their own branch
- auditors can read users globally in read-only mode
- only admins can mutate users across all branches and roles

### 4.4 Audit trail

The system records activity for:

- book views
- borrowing
- returns
- renewals
- reservation create and cancel
- reservation no-show
- fine waivers
- policy updates
- access updates

Book-view counting rules:

- `POST /api/books/{id}/view` counts at most one authenticated, non-auditor view per user and book until the next reset cycle
- the endpoint returns the authoritative current count plus a `counted` flag so the frontend does not rely on local `+1` assumptions
- the weekly reset script works by deleting `VIEWED` rows, which also clears those view entries from activity history

## 5. Frontend Structure

Implemented browser routes:

- `/`
  - landing page and discovery
- `/books`
  - books workspace
- `/upcoming`
  - upcoming acquisitions workspace
- `/books/:id`
  - book detail page
- `/me`
  - member self-service page
- `/admin`
  - operations workspace with sidebar task navigation

Key frontend areas:

- `WelcomePage`
- `BooksWorkspacePage`
- `UpcomingWorkspacePage`
- `BookDetailPage`
- `UserHubPage`
- `AdminPage`
- `AdminConsole`

## 6. Backend API Summary

### 6.1 Public endpoints

- `GET /api/books`
- `GET /api/books/filters`
- `GET /api/books/{id}`
- `GET /api/books/{id}/cover`
- `GET /api/discovery`
- `GET /api/upcoming-books`

### 6.2 Authenticated member endpoints

- `POST /api/borrowings`
- `POST /api/borrowings/{transactionId}/return`
- `POST /api/borrowings/{transactionId}/renew`
- `GET /api/borrowings/me`
- `POST /api/reservations`
- `GET /api/reservations/me`
- `POST /api/reservations/{reservationId}/cancel`
- `GET /api/fines/me`
- `GET /api/activity-logs/me`
- `GET /api/profile`
- `POST /api/books/{id}/view`
- `GET /api/inventory/digital-access/{transactionId}`

### 6.3 Operational endpoints

- `POST /api/books`
- `POST /api/books/{id}/cover`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`
- `GET /api/borrowings`
- `GET /api/reservations`
- `POST /api/reservations/{reservationId}/no-show`
- `GET /api/fines`
- `POST /api/fines/{fineId}/waive`
- `GET /api/activity-logs`
- `GET /api/branches`
- `GET /api/locations`
- `POST /api/locations`
- `PUT /api/locations/{locationId}`
- `GET /api/inventory/holdings`
- `POST /api/inventory/holdings`
- `PUT /api/inventory/holdings/{holdingId}`
- `GET /api/notifications`
- `POST /api/notifications`
- `POST /api/notifications/{notificationId}/read`
- `POST /api/upcoming-books`
- `PUT /api/upcoming-books/{upcomingBookId}`
- `DELETE /api/upcoming-books/{upcomingBookId}`
- `GET /api/users`
- `GET /api/users/{userId}`
- `GET /api/users/options`
- `GET /api/users/{userId}/options`
- `PUT /api/users/{userId}/access`
- `GET /api/users/{userId}/discipline`
- `POST /api/users/{userId}/discipline`
- `GET /api/policies/current`
- `PUT /api/policies/current`

## 7. Data Model Summary

Main tables:

- `app_user`
- `book`
- `book_tag`
- `book_cover`
- `book_holding`
- `borrow_transaction`
- `reservation`
- `fine_record`
- `library_policy`
- `activity_log`
- `library_branch`
- `library_location`
- `upcoming_book`
- `upcoming_book_tag`
- `staff_notification`
- `staff_notification_target_role`
- `staff_notification_receipt`
- `event_publication`

Key relationships:

- one user to many borrow transactions
- one user to many reservations
- one user to many fines
- one book to many borrow transactions
- one book to many reservations
- one book to many tags
- one book to many holdings
- one branch to many locations
- one location to many physical holdings
- one book to one current cover record

## 8. Seeded Data

The seeded dataset includes:

- a multi-title technical library catalog
- tag-based filtering data
- cover images
- branch-aware holdings and shelf locations
- digital books with online access URLs
- upcoming books
- staff notifications
- discovery ranking data
- borrow history
- reservations
- open and waived fines
- a global policy record
- enterprise access records
- discipline workflow tables and event logging support

Seeded realm accounts:

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

Seeded-user linkage notes:

- matching local `app_user` rows are seeded through Flyway and normalized to deterministic demo-user Keycloak ids by `V19__stabilize_demo_keycloak_ids.sql`
- the first authenticated backend request now usually matches directly by JWT subject
- automatic username relinking remains only as a compatibility path for legacy `seed-*` identities

Seeded member-state coverage:

- normal active members
  - `reader / reader123`
  - `alina.reader / reader123`
  - `central.member / reader123`
  - `east.member / reader123`
  - `hq.member / reader123`
- active member with restricted borrowing due to membership status
  - `hoang.nguyen / reader123` -> `ACTIVE` + `OVERDUE_RESTRICTED`
- active member with borrowing blocked
  - `maya.tran / reader123` -> `ACTIVE` + `BORROW_BLOCKED`
- no seeded login examples currently exist for:
  - `PENDING_VERIFICATION`
  - `SUSPENDED`
  - `LOCKED`
  - `ARCHIVED`
  - `EXPIRED`

Seeded branch coverage:

- `CENTRAL`: `branch.librarian`, `branch.manager`, `central.member`
- `EAST`: `east.librarian`, `east.manager`, `east.member`
- `HQ`: `hq.librarian`, `hq.manager`, `hq.member`

## 9. Security Notes

Implemented protections:

- Keycloak-based authentication
- role-to-permission resolution in the application
- status-aware borrowing restrictions
- self, branch, and global scope checks in service logic
- structured discipline workflow with reason capture for member suspension and ban actions
- anonymous access limited to intended public endpoints
- admin-only hard delete for books
- audit coverage across core circulation and access changes

Current limitations:

- local application access is authoritative after bootstrap, but there is no Keycloak admin synchronization flow yet
- registration is enabled in Keycloak, but self-registration does not yet complete usable library-account provisioning on its own
- branch-aware holdings exist, but full copy/barcode transfer workflows are still not implemented
- pickup-branch transfers and hold-routing rules are not implemented yet
- members cannot yet edit their profile or change passwords from the local application UI; those actions are delegated to Keycloak account management

## 10. Setup and Runtime

### 10.1 Full Docker stack

```bash
docker compose up -d --build
```

Clean reset:

```bash
docker compose down -v
docker compose up -d --build
```

### 10.2 Local backend workflow

Start supporting services:

```bash
docker compose up -d postgres keycloak otel-collector
```

Run backend:

```powershell
./mvnw spring-boot:run
```

Run frontend locally:

```bash
cd frontend
npm install
npm run dev
```

Important: do not run the Docker backend and the local JVM backend at the same time. They both need port `8080`.

### 10.3 Main URLs

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`

### 10.4 Public test mode

- Windows Command Prompt wrapper: `scripts\start-public-test.cmd -PublicUrl https://<your-ngrok-url>`
- tunnel only `http://localhost:3000`
- do not expose `8080` or `8081` directly
- the public frontend origin proxies `/api` to the backend and `/auth` to Keycloak, so testers only need one URL
- full how-to: `docs/NGROK_WINDOWS_SINGLE_PORT_SETUP.md`

## 11. Review Walkthrough

1. Open `http://localhost:3000`
2. Review the landing page discovery sections
3. Open `/upcoming`
4. Open `/books`
5. Search by title or filter by tag
6. Open a book detail page and inspect cover, metadata, and availability
7. Sign in as `reader`
8. Borrow or reserve an eligible title
9. Open `/me` and review borrowings, reservations, fines, and activity
10. Sign in as `branch.librarian`, `branch.manager`, `admin`, or `compliance.auditor`
11. Open `/admin` and verify the permission-specific operations panels

## 12. Delivery Status

The current implementation delivers:

- React frontend
- SQL-backed persistence
- REST API
- authentication plus Keycloak-hosted registration and account-management UI
- enterprise role model
- tag-aware catalog filtering
- cover image support
- borrow, return, renew, reserve, and fine flows
- operations workspace and access management
- audit history

The main remaining gaps are copy-level inventory, pickup-branch transfer workflows, full Keycloak admin synchronization, and complete self-registration provisioning.
