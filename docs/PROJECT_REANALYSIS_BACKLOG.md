# Project Re-analysis Backlog

Audit date: 2026-03-23

Audit basis:

- current backend modules, migrations, and tests
- current frontend SPA, route split, and test setup
- setup, operations, and public-test documentation
- verification commands run during this pass

Verification completed in this pass:

- `./mvnw.cmd test`
- `npm test` in `frontend`
- `npm run build` in `frontend`
- `npm run test:smoke` in `frontend`
- `cmd /c scripts\verify-migrations.cmd -EnvFile compose.env.example`

Verification notes:

- backend tests passed: 93 tests, 0 failures, 9 skipped
- skipped tests are the Testcontainers-backed integration suite because Docker is not currently reachable from this environment
- frontend unit tests passed
- frontend production build passed
- browser smoke tests passed
- migration verification failed against the currently running local database: latest repo migration is `21`, live schema version is `19`

## Recheck Of The Previous Backlog

Resolved since the previous rewrite:

- Windows Command Prompt, single-port `ngrok` setup is documented and scripted
- admin-only staff registration is implemented in the backend and admin UI
- database-backed integration coverage exists as a baseline
- frontend interaction coverage exists as a baseline
- browser smoke coverage exists as a baseline
- staff-desk flows now cover ready-hold checkout, override-with-reason, and item exceptions
- reservation fulfillment now uses a real transfer model
- physical inventory is now copy-backed rather than purely aggregate-count based
- `App.tsx` has already been partially decomposed through `AppView` and related frontend modules
- migration verification, backup/restore guidance, and env-based secret handling are implemented

Narrowed but still active:

- identity governance is improved, but repair and synchronization flows are still incomplete
- copy-level inventory exists, but copy-level operations are still thin
- transfer tracking exists, but operator-facing transfer handling is still shallow
- frontend structure is improved, but the main orchestration hotspot still lives in `frontend/src/App.tsx`
- operations hardening is better, but migration freshness and telemetry are not yet fully production-grade

## Current Findings

The highest-signal problems in the repo right now are:

- `docs/PROJECT_REANALYSIS_BACKLOG.md` was stale and still listed already shipped work as open
- `PROJECT_DOCUMENTATION.md` is now materially out of date and still claims copy-level transfer workflows are not implemented
- the currently running local database is behind the repo schema (`19` vs `21`)
- `frontend/src/App.tsx` is still the primary data-loading and mutation-orchestration hub even after the route/view split
- copy-backed inventory exists, but there is no dedicated per-copy admin workflow for receiving, reconciliation, repair, or manual exception handling
- transfer records exist, but the UI is still mostly a read-only transfer ledger rather than an operational transfer desk
- the integration and browser test layers exist, but they do not yet cover the most failure-prone circulation and transfer workflows
- the test suite emits a future-compatibility warning because Mockito still relies on dynamic self-attachment on JDK 25

## Priority 1: Correctness And Operational Trust

### P1.1 Update the stale project documentation

Current gap:

- `PROJECT_DOCUMENTATION.md` still says copy/barcode transfer workflows and pickup-branch transfer routing are not implemented
- the previous backlog document also still marked several completed items as open

Why it matters:

- the docs now misstate shipped behavior
- this creates review, onboarding, and operator error risk

Relevant areas:

- `PROJECT_DOCUMENTATION.md`
- `README.md`
- `SETUP_GUIDE.md`
- `docs/PROJECT_REANALYSIS_BACKLOG.md`

### P1.2 Finish identity repair and synchronization workflows

What is already in place:

- Keycloak owns authentication
- local `app_user` state owns role, status, and branch assignment
- staff accounts can be created only by admins
- non-member trust-on-first-login is blocked

What is still missing:

- an admin workflow to rebind a local account to a different Keycloak subject when recovery is required
- a drift check or repair view for mismatches between Keycloak identities and local access rows
- a documented authority model for what should happen if Keycloak role assignments drift away from local application state

Relevant areas:

- `src/main/java/com/example/library/identity/CurrentUserService.java`
- `src/main/java/com/example/library/identity/AccessManagementService.java`
- `src/main/java/com/example/library/identity/StaffRegistrationService.java`
- `infra/keycloak/realm-library.json`

### P1.3 Enforce migration freshness more aggressively

Current gap:

- the repo now has verification tooling, but the currently running local database is still behind the latest schema
- supported runtime modes still depend on operator sequencing rather than a stronger startup or release gate

Why it matters:

- the codebase and the running environment can silently diverge
- the transfer/copy-level changes in `V20` and `V21` are not optional if that environment is expected to reflect the current code

Relevant areas:

- `scripts/verify-migrations.cmd`
- `docs/OPERATIONS_RUNBOOK.md`
- `src/main/resources/db/migration`
- `compose.yaml`

## Priority 2: Workflow Depth

### P2.1 Build real copy-level operations, not just copy-level persistence

What exists:

- physical holdings now create and use tracked copies
- copies have barcodes, statuses, and branch/location state

What is still missing:

- dedicated per-copy admin endpoints for manual adjustments
- receiving, stock-check, and reconciliation workflows
- repair/quarantine/maintenance style handling
- better copy-history visibility for support staff

Relevant areas:

- `src/main/java/com/example/library/inventory/InventoryController.java`
- `src/main/java/com/example/library/inventory/InventoryService.java`
- `src/main/java/com/example/library/inventory/BookCopy.java`
- `frontend/src/components/InventoryPanel.tsx`

### P2.2 Turn transfer tracking into an operator workflow

What exists:

- transfers are persisted and exposed through the API
- reservation preparation and ready-for-pickup flows can drive transfer state

What is still missing:

- a receiving/worklist experience for branch staff
- explicit operator actions for transfer exceptions, cancellations, and late movement
- clearer transfer handling beyond the reservation-side happy path

Relevant areas:

- `src/main/java/com/example/library/circulation/TransferService.java`
- `src/main/java/com/example/library/circulation/TransferController.java`
- `frontend/src/components/TransfersPanel.tsx`
- `frontend/src/components/AdminConsole.tsx`

### P2.3 Complete post-exception desk workflows

What exists:

- ready-hold checkout, override renewals, and borrowing exceptions are implemented

What is still missing:

- follow-on settlement for lost or damaged items
- repair or disposition workflows after an item exception is recorded
- support for states beyond the current exception set such as maintenance or quarantine

Relevant areas:

- `src/main/java/com/example/library/circulation/CirculationService.java`
- `src/main/java/com/example/library/inventory/BookCopyStatus.java`
- `src/main/java/com/example/library/history/ActivityLogService.java`
- `frontend/src/components/AdminConsole.tsx`

## Priority 3: Frontend And Test Maturity

### P3.1 Finish decomposing `frontend/src/App.tsx`

Current hotspot:

- the file is still large and owns most state, data loading, refresh sequencing, and mutation handlers

Why it matters:

- the route/view split improved readability, but the orchestration burden is still concentrated in one file
- this is the clearest remaining frontend maintainability bottleneck

Relevant areas:

- `frontend/src/App.tsx`
- `frontend/src/app/AppView.tsx`
- `frontend/src/app/permissions.ts`
- `frontend/src/app/forms.ts`

### P3.2 Deepen integration and browser coverage around circulation

What exists:

- Postgres-backed integration test scaffolding exists
- frontend unit tests exist
- Playwright smoke tests exist

What is still missing:

- DB-backed integration tests for circulation, inventory, and transfer workflows
- browser tests for signed-in borrowing, reservations, ready holds, staff checkout, and transfer handling
- integration coverage for the Keycloak-admin staff provisioning path beyond service-level mocking

Relevant areas:

- `src/test/java/com/example/library/integration`
- `src/test/java/com/example/library/circulation`
- `frontend/e2e/library.smoke.spec.ts`
- `frontend/src/app/AppView.test.tsx`

### P3.3 Complete member onboarding and self-service account management

Current gap:

- self-registration in Keycloak still does not create a ready-to-use library member account
- profile/password management is still delegated out to Keycloak instead of being explained more clearly in-app

Relevant areas:

- `src/main/java/com/example/library/identity/CurrentUserService.java`
- `infra/keycloak/realm-library.json`
- `frontend/src/components/ProfilePanel.tsx`
- `frontend/src/components/UserHubPage.tsx`

## Priority 4: Operations And Tooling

### P4.1 Move observability beyond debug export

Current state:

- OTEL traces and metrics are wired into the collector
- the collector still exports only to the debug exporter

What is still missing:

- a real trace/metrics backend
- dashboards, useful labels, and alertable signals
- an operator story beyond local inspection

Relevant areas:

- `src/main/resources/application.yaml`
- `infra/otel/collector-config.yaml`
- `compose.yaml`

### P4.2 Future-proof the test runtime

Current gap:

- the test run on JDK 25 warns that Mockito is dynamically attaching an agent
- that behavior is scheduled to become disallowed by default in a future JDK

Why it matters:

- the suite is green today, but the build is carrying a predictable future-upgrade failure mode

Relevant areas:

- `pom.xml`
- `src/test/java`

## Recommended Execution Order

1. Update stale project documentation so it matches the shipped system.
2. Add identity rebind and drift-repair workflows for Keycloak/local access state.
3. Promote migration freshness from a helper script to a stricter operational gate.
4. Add real copy receiving, reconciliation, and manual copy-management operations.
5. Add operator-facing transfer desk workflows.
6. Continue breaking down `frontend/src/App.tsx` into domain-level data hooks and action modules.
7. Expand integration and browser coverage around signed-in circulation and transfer paths.
8. Move telemetry off the debug exporter and future-proof Mockito/JDK test startup.
