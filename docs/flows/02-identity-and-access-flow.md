# Identity And Access Flow

## Purpose

This module handles authentication handoff from Keycloak, current-user synchronization into `app_user`, role and scope resolution, profile retrieval, user access management, and discipline workflows.

## Actors

- Guest
- Authenticated member
- Librarian
- Branch manager
- Admin
- Auditor
- Keycloak
- Backend identity and notification modules

## Main Components

### Frontend

- `frontend/src/auth.ts`
- `frontend/src/App.tsx`
- `frontend/src/components/ProfilePanel.tsx`
- `frontend/src/components/AccessManagementPanel.tsx`

### Backend

- `SecurityConfig`
- `CurrentUserService`
- `AuthorizationService`
- `ProfileController`
- `AccessManagementController`
- `AccessManagementService`
- `NotificationController`
- `NotificationService`
- `AppUser`
- `AppUserRepository`

## Main Endpoints

- Keycloak login, registration, logout, reset-password, and account-management pages
- `GET /api/profile`
- `GET /api/users`
- `GET /api/users/options`
- `GET /api/users/{userId}`
- `GET /api/users/{userId}/options`
- `PUT /api/users/{userId}/access`
- `GET /api/users/{userId}/discipline`
- `POST /api/users/{userId}/discipline`
- `POST /api/notifications/discipline-requests`

## 1. Browser Authentication Flow

1. The frontend initializes Keycloak with `check-sso`.
2. If the user clicks `Login` or `Register`, the browser is redirected to Keycloak.
3. The custom library Keycloak theme renders the hosted auth pages.
4. After successful login, Keycloak redirects back to the frontend.
5. Protected API requests send `Authorization: Bearer <token>`.

In single-URL public-test mode, the frontend still uses the same flow, but Keycloak is reached through the same public origin under `/auth`.

## 2. Seeded Demo User Binding Flow

1. The first protected backend request reaches Spring Security.
2. `CurrentUserService` reads the JWT subject, preferred username, email, and authorities.
3. It looks up `app_user` by `keycloak_user_id`.
4. For shipped demo users, the seeded local row already carries the same deterministic Keycloak subject id as the imported realm user.
5. If no row matches by subject, `CurrentUserService` tries the username.
6. When the local row still uses a legacy `seed-*` identity, that row is rebound to the real Keycloak subject.
7. Username and email are synchronized from the token.
8. The local user is saved and returned as the current user.

This is how the shipped demo accounts stay aligned with the Flyway-seeded `app_user` rows even after Keycloak recreation.

## 3. New User Bootstrap Flow

1. If no local row matches by subject or username, `CurrentUserService` resolves the highest library role from JWT authorities and `realm_access.roles`.
2. A new local `AppUser` is auto-created only when that resolved role is `MEMBER`.
3. New staff and admin identities are rejected until they are locally provisioned.
4. Auto-created members default to `ACTIVE` + `GOOD_STANDING` with no branch or home-branch assignment.

Shipped-realm caveat:

- the exported realm also does not automatically grant a usable library member role to a brand-new self-registered user

## 4. Role, Scope, And Permission Resolution

Highest-role resolution order:

1. `ADMIN`
2. `AUDITOR`
3. `BRANCH_MANAGER`
4. `LIBRARIAN`
5. `MEMBER`

Scope mapping:

- `MEMBER` -> `SELF`
- `LIBRARIAN` -> `BRANCH`
- `BRANCH_MANAGER` -> `BRANCH`
- `ADMIN` -> `GLOBAL`
- `AUDITOR` -> `GLOBAL`

Important account rules:

- only `ACTIVE` accounts are treated as active for protected application actions
- only `GOOD_STANDING` members can start self-service borrowing, renewal, reservation creation, and reservation collection
- auditors are globally visible but read-only

## 5. Profile Flow

1. The frontend calls `GET /api/profile`.
2. `ProfileController` delegates to `CurrentUserService`.
3. Current-user synchronization runs before the response is built.
4. The response returns:
   - username
   - email
   - role
   - account status
   - membership status
   - scope
   - branch and home branch summaries
   - effective permission names
5. `/me` renders the profile card and exposes a Keycloak account-management link for profile/password edits.

## 6. Access Workspace Flow

1. A user with user-read capability opens the operations workspace.
2. The frontend calls `GET /api/users`, `GET /api/users/{id}`, `GET /api/users/{id}/options`, and `GET /api/users/{id}/discipline`.
3. `AccessManagementService` filters visible users according to the caller scope:
   - `ADMIN` and `AUDITOR` can read all users globally
   - `BRANCH_MANAGER` can read members and librarians in the same branch
   - `LIBRARIAN` does not receive user-list access
4. Permission visibility is narrower than user visibility:
   - users can see their own effective permission list
   - admins can see permissions for every user
   - branch managers can see permissions for manageable same-branch members and librarians
   - auditors can read users but do not see other users' permission maps

## 7. Access Update Flow

1. The frontend submits `PUT /api/users/{userId}/access`.
2. `AuthorizationService.assertCanManageUser(...)` validates role, scope, target branch, and allowed transitions.
3. `AccessManagementService` resolves the requested branch and home branch, updates the local `AppUser`, and publishes an access-update event.

Current update boundaries:

- `ADMIN`
  - can change roles, account statuses, membership statuses, branch, and home branch globally
- `BRANCH_MANAGER` on same-branch `MEMBER`
  - role is fixed
  - account status can be `PENDING_VERIFICATION`, `ACTIVE`, or `SUSPENDED`
  - membership status can be changed
- `BRANCH_MANAGER` on same-branch `LIBRARIAN`
  - role is fixed
  - account status can be `ACTIVE` or `SUSPENDED`
  - membership status stays unchanged

## 8. Discipline Flow

Direct discipline actions:

- `SUSPEND` -> `SUSPENDED`
- `BAN` -> `LOCKED`
- `REINSTATE` -> `ACTIVE`

Transition rules:

- only `ACTIVE` users can be suspended or banned
- only `SUSPENDED` or `LOCKED` users can be reinstated
- users cannot discipline their own accounts

Who can act:

- `ADMIN`
  - can discipline any other user globally
- `BRANCH_MANAGER`
  - can discipline same-branch `MEMBER` and `LIBRARIAN` accounts
- `LIBRARIAN`
  - cannot apply discipline directly
  - can submit `POST /api/notifications/discipline-requests` for a same-branch member
  - recipients are same-branch branch managers plus all admins

All applied discipline actions are written to `user_discipline_record` and also emitted into the operational activity log.

## Business Rules

- Only authenticated users can access protected identity endpoints.
- The application trusts Keycloak for authentication, but local `app_user` data remains authoritative for role, branch, and status checks after synchronization.
- Brand-new local bootstrap is intentionally limited to `MEMBER` identities.
- New member bootstrap uses server-side defaults instead of trusting branch or status data from JWT claims.
- The shipped self-registration UI does not complete library-account provisioning on its own.
- Branch-scoped staff can act only inside their branch.
- Auditors are globally read-only.

## Affected Tables

- `app_user`
- `library_branch`
- `user_discipline_record`
- `staff_notification`
- `staff_notification_receipt`
- `activity_log`
