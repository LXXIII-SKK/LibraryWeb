# Project Re-analysis Backlog

Audit date: 2026-03-23

Audit basis:

- current backend modules and migrations
- current frontend SPA and startup config
- setup documentation and helper scripts
- automated checks run during this review

Verification completed in this pass:

- `./mvnw.cmd test`
- `npm run build` in `frontend`
- `.\scripts\start-public-test.ps1 -PublicUrl https://example.ngrok-free.app -WhatIf`
- `cmd /c scripts\start-public-test.cmd -PublicUrl https://example.ngrok-free.app -WhatIf`

## Current Snapshot

What is clearly in good shape right now:

- backend unit and modulith tests pass
- frontend production build passes
- the public-test path for one-origin `ngrok` exposure is documented and the script dry-run behaves as expected
- staff/admin trust-on-first-login is blocked
- reservation workflow now covers pickup branch, prepare, ready, collect, no-show, and expire states
- deterministic demo Keycloak subject IDs are now part of the local story
- weekly discovery ranking now uses database-backed queries
- controller-level security tests now cover representative public and protected HTTP paths
- core mutation audit coverage now includes catalog, branch, location, holding, upcoming-book, and notification activity
- CI now enforces backend tests plus frontend build
- runtime verification now has a single command for Docker or local-Vite URL checks
- the OTEL collector now accepts metrics as well as traces

What changed during this review:

- local frontend startup was normalized so `npm run dev` now matches the documented Vite port `5173`
- frontend API fallback now uses `http://localhost:8080` when `VITE_API_BASE_URL` is unset
- explicit blank `VITE_API_BASE_URL` still keeps same-origin `/api` mode available for the single-origin public-test flow
- `scripts\verify-runtime.cmd` now validates frontend, backend, and Keycloak URLs in one command
- `NotificationService` now queries only visible notifications instead of loading the full notification table
- GitHub Actions CI now runs backend tests and frontend build

## Closed Or Verified

These items should not stay on the active backlog:

- one-origin `ngrok` public-test setup
- frontend production build health
- backend unit-test suite health
- deterministic demo-user Keycloak ID alignment
- duplicate book-view suppression within the current reset cycle
- query-based notification visibility loading
- baseline controller/security integration tests
- one-command runtime verification
- baseline CI enforcement
- OTEL collector metrics ingestion support

## Priority 1: Reliability And Control Plane

### P1.1 Runtime verification is better, but startup modes are still not fully automated

Status now:

- the immediate config mismatch was fixed in this pass
- `scripts\verify-runtime.cmd` now validates the documented frontend, backend, and Keycloak URLs together

Remaining gap:

- there is still no automated end-to-end workflow that boots each supported runtime mode and proves user-visible flows
- local JVM mode, Docker mode, and public-test mode still rely on operator sequencing

Why it matters:

- this project has multiple runtime modes: Docker, local JVM, local Vite, and public test through `ngrok`
- configuration drift is likely to reappear without a lightweight startup check

Relevant areas:

- `frontend/src/api.ts`
- `frontend/vite.config.ts`
- `SETUP_GUIDE.md`

### P1.2 Identity governance is still split across Keycloak and local access state

What is implemented:

- Keycloak owns authentication
- local `app_user` data owns role, account status, membership status, and branch assignment
- automatic first-login provisioning is limited to member identities
- automatic username relink is limited to seeded demo identities

What is missing:

- an explicit provisioning flow for staff and admin users
- a supported reassignment/rebinding workflow when a real identity changes in Keycloak
- a synchronization strategy between Keycloak role data and local access state

Why it matters:

- the current model is safe enough for demos, but still operationally fragile
- production support would depend on manual coordination across two authority systems

Relevant areas:

- `src/main/java/com/example/library/identity/CurrentUserService.java`
- `src/main/java/com/example/library/identity/AccessManagementService.java`
- `infra/keycloak/realm-library.json`

### P1.3 Security confidence is improved, but still not full-stack

What is verified:

- service tests exist for catalog, circulation, discovery, history, identity, and notifications
- modulith structure verification exists
- controller/security tests now cover representative protected and public endpoints

What is still missing:

- database-backed integration tests for Flyway and repository behavior
- committed frontend interaction tests
- browser end-to-end coverage for login, borrowing, reservations, and access-management flows

Why it matters:

- most remaining risk is now in cross-layer authorization and workflow integration
- the current test suite does not prove the HTTP boundary is enforcing the intended rules

Relevant areas:

- `src/test/java/com/example/library`
- `frontend/package.json`

### P1.4 Audit coverage is broader, but still not forensic-grade

What is logged already:

- borrow, return, renew, reservation, fine waiver, access, policy, and book-view activity
- catalog, branch, location, holding, upcoming-book, and notification create/read activity

What is not yet covered well:

- copy-level state transitions
- settlement and exception workflows that do not yet exist in the domain
- richer structured before/after payloads beyond human-readable messages

Why it matters:

- the product now supports more operational changes than the audit log can fully explain
- this is a control and supportability gap, not just a reporting gap

Relevant areas:

- `src/main/java/com/example/library/history/ActivityLogService.java`
- `src/main/java/com/example/library/branch`
- `src/main/java/com/example/library/inventory`
- `src/main/java/com/example/library/notification`
- `src/main/java/com/example/library/upcoming`

## Priority 2: Workflow Depth

### P2.1 Staff circulation is usable but not service-desk complete

Missing next:

- ready-reservation checkout completion in the staff UI
- due-date or renewal override with reason capture
- lost, damaged, claimed-returned, and maintenance states
- settlement handling for damaged or lost items

Relevant areas:

- `src/main/java/com/example/library/circulation/CirculationService.java`
- `frontend/src/components/AdminConsole.tsx`
- `src/main/java/com/example/library/circulation/BorrowStatus.java`

### P2.2 Reservation transfer is still logical rather than physical

Missing next:

- transfer entity or queue
- explicit movement timestamps
- transfer worklists
- ETA calculation
- real holding branch/location movement during transfer

Relevant areas:

- `src/main/java/com/example/library/circulation/ReservationService.java`
- `src/main/java/com/example/library/circulation/Reservation.java`
- `src/main/java/com/example/library/inventory/BookHolding.java`

### P2.3 Inventory is still title-and-quantity based, not copy based

Missing next:

- barcode or item identity
- per-copy condition and status
- copy-level transfer history
- stock-check and reconciliation flows
- item-level auditability

Relevant areas:

- `src/main/java/com/example/library/inventory/InventoryService.java`
- `src/main/java/com/example/library/inventory/BookHolding.java`

### P2.4 Branch, location, and holding lifecycle remains CRUD-light

Missing next:

- branch closure workflow
- archive/retire rules
- quarantine/repair/cart style locations
- location retirement rules
- holding retirement history

Relevant areas:

- `src/main/java/com/example/library/branch/BranchService.java`
- `src/main/java/com/example/library/inventory/LocationService.java`
- `src/main/java/com/example/library/inventory/InventoryService.java`

## Priority 3: Product And Frontend Maturity

### P3.1 The frontend shell is still too centralized

Observed hotspot:

- `frontend/src/App.tsx` still owns a large share of route wiring, permission derivation, data loading, and mutation refresh orchestration

Why it matters:

- the UI currently works, but this is the clearest maintainability bottleneck on the frontend side

Relevant areas:

- `frontend/src/App.tsx`

### P3.2 Member self-service is functional but still thin on guidance

Missing next:

- richer explanations for account restrictions
- clearer recovery and password guidance inside the app shell
- reminder scheduling
- better borrowing and reservation status messaging

Relevant areas:

- `frontend/src/components/UserHubPage.tsx`
- `frontend/src/components/ProfilePanel.tsx`
- `src/main/java/com/example/library/notification`

### P3.3 Notifications are useful, but not yet a fuller communications system

Missing next:

- admin-authored direct user targeting in the UI
- priority/severity
- scheduled reminders
- channels beyond in-app

Relevant areas:

- `frontend/src/components/NotificationsPanel.tsx`
- `frontend/src/components/NotificationTray.tsx`
- `src/main/java/com/example/library/notification/NotificationService.java`

### P3.4 Upcoming books are still acquisition-lite

Missing next:

- approval workflow
- vendor/source metadata
- procurement status
- cancel/defer with reasons
- conversion of upcoming entries into catalog books and holdings

Relevant areas:

- `src/main/java/com/example/library/upcoming/UpcomingBookService.java`
- `frontend/src/components/UpcomingBooksPanel.tsx`

## Priority 4: Delivery And Operations

### P4.1 CI baseline exists, but environment hardening remains unfinished

Missing next:

- migration verification in CI
- backup/restore guidance
- documented secret handling beyond local demo defaults

Relevant areas:

- `pom.xml`
- `frontend/package.json`
- `compose.yaml`

### P4.2 Observability is cleaner, but still minimal

Known state:

- tracing is wired for container runs
- the collector now accepts metrics as well as traces
- the environment still exports only to debug-style collector output and is not yet an operations-grade telemetry setup

Relevant areas:

- `src/main/resources/application.yaml`
- `infra/otel/collector-config.yaml`
- `compose.yaml`

## Recommended Execution Order

1. Design the staff/admin provisioning flow between Keycloak and local access state.
2. Add database-backed integration coverage plus browser-level smoke tests.
3. Complete staff-desk workflows around ready holds, overrides, and item exceptions.
4. Introduce a real transfer model, then move toward copy-level inventory.
5. Break down `App.tsx` and add frontend interaction coverage.
6. Harden operations with migration verification, backup/restore guidance, and secret management.

## ngrok Reference

Existing `ngrok` startup guidance already exists here:

- `README.md`
- `SETUP_GUIDE.md`
- `PROJECT_DOCUMENTATION.md`
- `scripts/README.md`

Current recommendation:

- treat `scripts/README.md` as the shortest operational reference
- treat `SETUP_GUIDE.md` as the broader onboarding guide
