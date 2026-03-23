# Frontend Web Flows

## Purpose

This document describes the current route behavior in the React application and shows how authentication and permissions shape what each page loads.

## Main Screens

- welcome page
- books workspace
- book detail page
- my page
- operations workspace

## 1. App Bootstrap Flow

1. The browser loads `http://localhost:3000`.
2. React resolves route state from the current pathname.
3. Keycloak silent SSO runs through `initAuth()`.
4. Public data loads for every visitor:
   - discovery feed
   - catalog books
   - filter metadata
   - upcoming books
   - public branch list
5. After authentication, the frontend loads `/api/profile`.
6. Additional private requests are then selected from the returned permission set, including:
   - personal borrowings, reservations, fines, and activity
   - notifications
   - operational borrowings, reservations, fines, users, policy, branches, holdings, locations, and activity

In single-origin public-test mode, the same browser shell serves the SPA and reaches the backend under `/api` plus Keycloak under `/auth`.

## 2. Public Browsing Flow

Guests can:

- browse `/`
- browse `/books`
- browse `/upcoming`
- open `/books/:id`
- trigger Keycloak login or registration
- open `/me` and see a sign-in prompt
- open `/admin` and see the restricted-state message

Guests cannot:

- call protected APIs
- borrow, renew, reserve, or collect
- open notifications
- access operations panels

## 3. Login, Registration, And Account Management Flow

1. The user clicks `Login` or `Register` in the top bar or empty-state page.
2. The browser navigates to Keycloak.
3. The custom library Keycloak theme renders the hosted forms.
4. After authentication, the browser returns to the React app.
5. The app reloads profile data and the permission-scoped private datasets.
6. From `/me`, the `Manage profile & password` action opens Keycloak account management.

Important caveat:

- the shipped realm exposes registration, but a brand-new signup is not automatically provisioned into a usable library account

## 4. Welcome And Books Flow

`/`:

- shows the recommendation hero first
- renders most-borrowed, most-viewed, and upcoming sections in 4-card pages
- keeps the upcoming section at the end of the page
- exposes a shortcut into the dedicated upcoming workspace

`/books`:

- supports query, category, and tag filtering
- paginates the visible catalog in groups of 4 titles
- shows current quantity and online-access hints
- enables borrow and reserve actions only when the signed-in profile is currently eligible

`/upcoming`:

- provides a dedicated acquisitions page separate from the live catalog
- supports client-side search and branch filtering
- paginates the visible upcoming titles in groups of 4

`/books/:id`:

- loads the full book response
- records a book view for authenticated non-auditors
- uses the backend-returned count so the UI does not double-increment in local development
- counts at most one view per signed-in user and book until the next reset
- shows branch-aware holdings, recent related activity, and pickup-branch options

## 5. My Page Flow

`/me` serves both anonymous and signed-in states.

Anonymous state:

- sign-in/register call to action
- summary of what the account workspace contains

Signed-in state:

- profile card with role, statuses, scope, branch, home branch, and permission count
- borrowings with renew, return, and digital-access actions when allowed
- reservations with cancel and collect actions when allowed
- fines and overdue context
- targeted notifications with mark-read action
- personal activity history

## 6. Operations Workspace Flow

`/admin` is not admin-only. It is an operations workspace rendered whenever the current permission set grants at least one operational capability.

Typical visibility by role:

- `LIBRARIAN`
  - catalog and inventory work
  - discipline-review request form
- `BRANCH_MANAGER`
  - branch-scoped circulation, fines, notifications, users, policies, and inventory
- `ADMIN`
  - full operational control, including branch management, global user management, and hard delete for books
- `AUDITOR`
  - global read-only operations review

If a signed-in user lacks operational permissions, `/admin` shows a restricted-state page instead of the console.

## 7. Notifications Flow

- Active signed-in users can read visible notifications through the top-bar tray and the `/me` page.
- Branch managers and admins can send staff notifications from the operations workspace.
- Librarians can create discipline-review requests for same-branch members, which are delivered as notifications to branch managers and admins.

## 8. Validation And Refresh Flow

- React form state prevents incomplete or invalid operational submissions.
- Keycloak-hosted pages handle their own login and registration validation.
- After each successful mutation, the frontend refreshes the affected datasets instead of relying on stale local assumptions.

## 9. Frontend Structure Notes

Main orchestration lives in `frontend/src/App.tsx`.

Supporting areas:

- `frontend/src/auth.ts`
  - Keycloak lifecycle and token refresh
- `frontend/src/api.ts`
  - REST request helpers
- `frontend/src/components`
  - page and panel rendering
- `frontend/src/lib`
  - formatting and validation helpers
