# Project Re-analysis Backlog

Audit date: 2026-03-20
Audit basis: current backend modules, frontend SPA, migrations, test suite, Keycloak config, and the latest reservation/notification/branch workflow changes
Audit intent: capture what was closed in the latest implementation pass, what remains unfinished, and the most credible next upgrade paths

## Current Baseline

The system is now materially stronger than the previous audit baseline:

- privileged trust-on-first-login is blocked for staff and admin identities
- branch-scoped user visibility is narrowed to members unless stronger branch authority exists
- branch metadata is scoped for branch staff, while public branch summaries support pickup selection
- notification reads are controller-gated and now serve both staff and members
- unsafe SVG cover uploads are blocked
- seeded demo users can relink only from explicit seed identities, not from arbitrary username reuse
- staff checkout exists
- reservations support pickup branch, in-transit, ready-for-pickup, collect, expire, and member notifications
- the frontend exposes those flows through the catalog, detail page, user hub, and operations workspace
- backend tests pass and the frontend production build passes

## Closed In This Pass

These are no longer active backlog items:

- privileged runtime bootstrap for non-member identities
- branch-scoped overexposure of same-branch staff accounts to ordinary librarian-grade reads
- broad branch metadata leakage through the secured branch list
- soft authorization on notification read endpoints
- raw SVG cover upload acceptance
- missing staff checkout workflow
- missing pickup-branch reservation workflow
- missing ready-for-pickup and hold-expiry states
- missing member notification center
- missing account-management escape hatch from the member page

## Remaining Confirmed Security And Control Gaps

### A1. Identity governance is still split across Keycloak and local access control

What is true now:

- authentication is still Keycloak-owned
- role/status/scope enforcement is still local `app_user` owned
- privileged first-login bootstrap is blocked
- automatic username relink is now limited to seeded demo identities only

What is still missing:

- an explicit admin provisioning or sync workflow between Keycloak and local access
- deterministic realm reprovisioning for resets and CI
- a production-grade answer for staff onboarding, staff reassignment, and subject rebinding

Why it matters:

- the system is safer than before, but it still relies on operational knowledge across two authority planes
- the current seed-relink rule is acceptable for demo data, not as a long-term enterprise identity strategy

Relevant areas:

- `src/main/java/com/example/library/identity/CurrentUserService.java`
- `src/main/java/com/example/library/identity/AccessManagementService.java`
- `infra/keycloak/realm-library.json`

### A2. Audit coverage is still not forensic-grade

What exists:

- borrow, return, renew, reservation, fine waiver, policy, access, and view actions are logged

What is still missing:

- branch mutation audit
- location mutation audit
- holding mutation audit
- upcoming-book mutation audit
- notification create/read audit
- before/after state capture for important catalog and access changes

Why it matters:

- the platform now supports more operational actions than the audit trail currently reflects
- compliance review is still weaker than the user-facing feature set suggests

Relevant areas:

- `src/main/java/com/example/library/history/ActivityLogService.java`
- `src/main/java/com/example/library/branch`
- `src/main/java/com/example/library/inventory`
- `src/main/java/com/example/library/notification`
- `src/main/java/com/example/library/upcoming`

### A3. Security validation is still too unit-test heavy

What exists:

- service-level tests for catalog, circulation, discovery, history, and identity
- modulith structure verification

What is still missing:

- controller-level security integration tests
- database-backed integration tests for migrations and persistence rules
- committed frontend interaction tests
- browser end-to-end coverage for login, borrowing, reservation, and access-management flows

Why it matters:

- current risk has moved from pure domain logic to cross-layer authorization and workflow integration
- unit coverage alone is no longer enough

## Remaining Unfinished Product And Workflow Areas

### B1. Staff circulation is better, but still not a full service-desk workflow

What exists now:

- staff checkout to a selected member
- force return
- renewals
- reservation preparation and ready-for-pickup handling

What is still missing:

- staff checkout from a selected ready reservation in the UI
- override-with-reason for exceptional due dates or renewals
- lost, damaged, claimed-returned, and maintenance workflows
- settlement handling for damaged or lost physical items
- richer transaction status lifecycle beyond `BORROWED` and `RETURNED`

Relevant areas:

- `src/main/java/com/example/library/circulation/CirculationService.java`
- `frontend/src/components/AdminConsole.tsx`
- `src/main/java/com/example/library/circulation/BorrowStatus.java`

### B2. Reservation fulfillment is functional, but transfer is still logical rather than physical

What exists now:

- pickup branch choice
- in-transit status
- ready-for-pickup status
- expiration
- collect flow
- member notifications

What is still missing:

- a transfer entity or transfer queue
- transit timestamps beyond the reservation state itself
- explicit physical movement recording between branches and locations
- ETA calculation and transfer staff worklists
- holding current-branch mutation during real transfer operations

Why it matters:

- the workflow is operationally usable, but inventory movement is still represented indirectly
- this is the biggest remaining gap between the current reservation feature and a truly enterprise branch network

Relevant areas:

- `src/main/java/com/example/library/circulation/ReservationService.java`
- `src/main/java/com/example/library/circulation/Reservation.java`
- `src/main/java/com/example/library/inventory/BookHolding.java`

### B3. Inventory is still holding-level, not copy-level

What exists now:

- branch-aware holdings
- location-aware physical holdings
- digital holdings with access URLs
- quantity-based availability

What is still missing:

- barcode or copy identity
- per-copy status
- condition history
- copy-level transfer lifecycle
- stock-check and reconciliation workflows
- item-level audit trail

Why it matters:

- the system can answer where a title is available
- it still cannot answer which exact copy is where, in what condition, and with what movement history

Relevant areas:

- `src/main/java/com/example/library/inventory/InventoryService.java`
- `src/main/java/com/example/library/inventory/BookHolding.java`

### B4. Branch, location, and holding lifecycle is still CRUD-light

What exists now:

- create and update for branches, locations, and holdings
- active flags
- branch-scoped inventory management

What is still missing:

- archive or delete strategy with safety rules
- branch closure workflow
- temporary storage, quarantine, repair, and cart locations
- location retirement workflow
- holding retirement history

Relevant areas:

- `src/main/java/com/example/library/branch/BranchService.java`
- `src/main/java/com/example/library/inventory/LocationService.java`
- `src/main/java/com/example/library/inventory/InventoryService.java`

### B5. Member self-service is improved, but still partially delegated rather than modeled

What exists now:

- personal borrowings, fines, reservations, and notifications
- collect-ready-reservation flow
- manage-account handoff to Keycloak

What is still missing:

- first-class in-app profile editing
- explicit password-change and recovery guidance inside the app shell
- richer member-facing explanation of account restrictions and recovery paths
- overdue reminder scheduling
- more informative reservation and borrowing lifecycle messaging

Relevant areas:

- `frontend/src/components/UserHubPage.tsx`
- `frontend/src/components/ProfilePanel.tsx`
- `src/main/java/com/example/library/notification`

### B6. Notifications are useful, but still not a full communications system

What exists now:

- branch-scoped and role-scoped notification broadcasting
- member visibility
- system-driven direct-to-user notifications for reservation events
- read receipts

What is still missing:

- admin-authored direct user-targeted notifications
- scheduled reminders
- severity or priority
- channel expansion beyond in-app
- audit logging for notification create/read

Relevant areas:

- `src/main/java/com/example/library/notification/NotificationService.java`
- `frontend/src/components/NotificationsPanel.tsx`
- `frontend/src/components/NotificationTray.tsx`

### B7. Upcoming books remain acquisition-lite

What exists now:

- create, update, delete
- expected date, branch, tags, and summary

What is still missing:

- approval workflow
- procurement status
- vendor/source metadata
- conversion of an upcoming title into a catalog title plus holdings
- cancellation or deferment with reasons

Relevant areas:

- `src/main/java/com/example/library/upcoming/UpcomingBookService.java`
- `frontend/src/components/UpcomingBooksPanel.tsx`

## Frontend And Architecture Gaps

### C1. `App.tsx` still owns too much policy and reload orchestration

The frontend is functional, but the application shell still centralizes:

- capability derivation
- public/private data loading
- mutation refresh strategy
- route-level wiring for many feature modules

This is workable for the current size, but it is now the biggest frontend maintainability hotspot.

Relevant area:

- `frontend/src/App.tsx`

### C2. Operations panels are usable, but not yet lifecycle-complete

The task sidebar is a real improvement. The remaining issue is depth:

- reservations do not yet include staff completion from ready pickup
- inventory does not yet show transfer or condition lifecycle
- access management still does not support per-user custom permissions
- notifications do not yet support direct user targeting in the UI

Relevant areas:

- `frontend/src/components/AdminConsole.tsx`
- `frontend/src/components/ReservationsPanel.tsx`
- `frontend/src/components/InventoryPanel.tsx`
- `frontend/src/components/AccessManagementPanel.tsx`
- `frontend/src/components/NotificationsPanel.tsx`

## Delivery And Platform Gaps

### D1. CI and environment hardening are still missing

What is still missing:

- CI pipeline
- backend test and frontend build enforcement in CI
- migration verification in CI
- deterministic Keycloak reprovisioning
- documented production secret handling
- backup and restore guidance

Relevant areas:

- `pom.xml`
- `frontend/package.json`
- `compose.yaml`
- `infra/keycloak`

### D2. Observability still needs cleanup

Known state from prior runtime checks:

- OTEL export has shown connection failures in container runs
- metrics and telemetry are present, but not yet clean enough to be trusted as an operational baseline

Relevant areas:

- `src/main/resources/application.yaml`
- `infra/otel/collector-config.yaml`
- `compose.yaml`

## Suggested Execution Order

If the goal is to keep tightening enterprise quality, the highest-value order now is:

1. build deterministic Keycloak-to-local provisioning and staff onboarding
2. add controller/security integration tests and a small browser smoke suite
3. complete staff desk workflows for ready reservations, overrides, and lost/damaged handling
4. introduce a real transfer model for branch fulfillment
5. move from holding-level to copy-level inventory
6. expand audit coverage for inventory, notifications, branches, locations, and acquisitions
7. add CI, migration validation, and environment hardening
8. then deepen acquisitions, communications, and digital-resource maturity

## Potential Upgrade Paths

### Path 1. Operational hardening

Best if the next milestone is enterprise readiness:

- deterministic IAM provisioning
- controller integration tests
- CI
- audit expansion
- observability cleanup

### Path 2. Real circulation network

Best if the next milestone is branch realism:

- ready-reservation desk completion
- transfer queue
- transit lifecycle
- copy-level inventory
- condition and settlement workflows

### Path 3. Patron experience

Best if the next milestone is member-facing polish:

- clearer restriction messaging
- scheduled reminders
- richer notification center
- in-app profile flows
- smoother pickup and digital-access guidance

### Path 4. Collection management

Best if the next milestone is librarian and manager depth:

- acquisition approvals
- vendor metadata
- upcoming-to-catalog conversion
- archive and retirement flows
- inventory reconciliation

## Summary

The last major backlog is no longer current. The security posture and reservation workflow are materially better now.

The remaining work is concentrated in four real areas:

- authoritative identity governance across Keycloak and local access
- deeper desk and transfer workflows
- copy-level inventory and stronger auditability
- delivery maturity through tests, CI, and environment hardening

That means the next pass should focus less on broad feature count and more on operational depth and control-plane quality.
