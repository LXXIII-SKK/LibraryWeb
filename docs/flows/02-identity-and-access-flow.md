# Identity And Access Flow

## Purpose

This module handles authentication, registration, user synchronization, enterprise role detection, account status enforcement, branch scope enforcement, and profile retrieval.

## Actors

- Guest
- Authenticated member
- Librarian
- Branch manager
- Admin
- Auditor
- Keycloak
- Backend identity module

## Main Components

### Frontend

- `frontend/src/auth.ts`
- `frontend/src/App.tsx`

### Backend

- `CurrentUserService`
- `ProfileController`
- `AppUser`
- `AppUserRepository`

## Main Endpoints

- Keycloak login and registration endpoints
- `GET /api/profile`

## Registration And Login Flow

1. User opens the frontend.
2. React initializes Keycloak with silent SSO check.
3. If not authenticated, the user can click `Login` or `Register`.
4. Browser is redirected to Keycloak.
5. After successful login, Keycloak redirects back to the frontend.
6. Frontend stores the access token in the Keycloak client instance.
7. All protected backend requests include `Authorization: Bearer <token>`.

## Current User Synchronization Flow

1. A protected backend endpoint is called.
2. Spring Security parses the JWT token.
3. `CurrentUserService.getCurrentUser()` reads:
   - subject
   - preferred username
   - email
   - realm roles
4. The service checks whether the user already exists in `app_user`.
5. If found:
   - username, email, and role are synchronized
6. If not found:
   - a new `AppUser` row is created
7. The synchronized user is returned as `CurrentUser`.

## Role, Status, And Scope Resolution Flow

1. Authorities are extracted from JWT realm roles.
2. The backend resolves the highest enterprise role present:
   - `ROLE_ADMIN`
   - `ROLE_AUDITOR`
   - `ROLE_BRANCH_MANAGER`
   - `ROLE_LIBRARIAN`
   - `ROLE_MEMBER`
3. Account state is resolved separately through:
   - `account_status`
   - `membership_status`
4. Scope is resolved from role and branch assignment:
   - `MEMBER` -> `SELF`
   - `LIBRARIAN` -> `BRANCH`
   - `BRANCH_MANAGER` -> `BRANCH`
   - `ADMIN` -> `GLOBAL`
   - `AUDITOR` -> `GLOBAL`
5. Branch identifiers are synchronized from JWT claims if present and otherwise preserved from the database.

## Profile Flow

1. Frontend calls `GET /api/profile`.
2. `ProfileController` calls `CurrentUserService.getCurrentUser()`.
3. The current user is synchronized to `app_user`.
4. `ProfileResponse` is returned.
5. Frontend displays username, email, role, account status, membership status, scope metadata, and effective permissions.

## Business Rules

- Only authenticated users can access protected endpoints.
- Permissions are derived from role, but sensitive actions are also gated by account status and scope.
- `MEMBER` access is self-only for personal borrowing data.
- Branch-scoped staff can only operate on records inside their branch.
- `AUDITOR` is strictly read-only.
- User profile data is synchronized on demand.

## Affected Tables

- `app_user`
