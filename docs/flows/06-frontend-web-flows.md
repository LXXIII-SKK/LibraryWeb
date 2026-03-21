# Frontend Web Flows

## Purpose

This document describes the current website behavior from the user perspective and maps each major page to its underlying flow.

## Main Screens

- Welcome page
- Books workspace
- Book detail page
- User account page
- Admin page

## 1. Website Load Flow

1. User opens `http://localhost:3000`.
2. React initializes the app and route state.
3. Keycloak silent SSO check runs.
4. The app loads public data:
   - discovery feed
   - book catalog
5. If the user is authenticated, it also loads:
   - profile
   - personal borrowings
   - personal activity logs
6. If the user is admin, it also loads:
   - all borrowings
   - all activity logs

## 2. Welcome Page Flow

Route:

- `/`

What the page shows:

- recommendation section
- most borrowed books this week
- most viewed books this week
- entry points into the rest of the app

Flow:

1. Frontend requests `/api/discovery`.
2. Backend aggregates discovery sections.
3. Discovery cards render on the page.
4. User can open a specific book from a discovery card.

## 3. Guest Browsing Flow

Guests can:

- open the welcome page
- browse the books page
- search by title, author, or category
- open a book detail page
- inspect stock and metadata

Guests cannot:

- borrow books
- open `/me`
- use admin actions
- open `/admin`

## 4. Login And Registration Flow

1. User clicks `Login` or `Register`.
2. Browser navigates to Keycloak.
3. Custom library-themed Keycloak pages render.
4. Inline validation warns on invalid or empty fields as the user leaves them.
5. After successful authentication, the browser returns to the app.
6. The frontend reloads protected data and role-dependent UI.

## 5. Books Workspace Flow

Route:

- `/books`

What the page does:

- browse catalog
- search and filter
- choose a due date
- borrow directly from the list
- open a specific book detail page

Flow:

1. User types into the search or filter controls.
2. Frontend requests `GET /api/books`.
3. Matching books render as cards.
4. Signed-in users can choose a due date and borrow from the card.
5. On success, the app refreshes catalog, borrowings, and activity data immediately.

## 6. Book Detail Flow

Route:

- `/books/:id`

What the page shows:

- title, author, category, ISBN
- total and available quantity
- current availability state
- recent activity context
- borrow action for signed-in users

Flow:

1. User opens a book card or detail link.
2. Frontend requests `GET /api/books/{id}`.
3. If authenticated, frontend also posts `POST /api/books/{id}/view`.
4. Book details render on the page.
5. If the user borrows from this page, the app refreshes both shared data and the currently open book detail record immediately.

## 7. User Account Flow

Route:

- `/me`

What the page shows:

- current user profile
- active and past borrowings
- due dates
- personal activity history

Flow:

1. Signed-in user opens `/me`.
2. Frontend loads `/api/profile`, `/api/borrowings/me`, and `/api/activity-logs/me`.
3. The page renders current loans and historical activity.
4. User can return eligible books from this page.
5. On success, the page refreshes immediately.

## 8. Admin Page Flow

Route:

- `/admin`

Access:

- admin users only

What the page shows:

- create and edit book form
- delete book action
- full borrowings list
- force return controls
- system-wide activity log

Flow:

1. Admin opens `/admin`.
2. Frontend verifies role-based access before rendering the page.
3. Admin actions call protected book, borrowing, and activity endpoints.
4. Non-admin users do not see the nav link and are blocked from using the page directly.

## 9. Form Validation Flow

The web app validates inputs before submit and also when the user leaves a field.

React-side forms:

- admin book form
- due-date inputs on borrowing actions

Keycloak-hosted forms:

- login
- registration

Behavior:

- empty or invalid values trigger inline field errors
- invalid due dates block borrowing
- invalid book form data blocks create and update actions

## 10. Frontend Structure Notes

The UI is organized by page and focused components, with orchestration centered in `App.tsx`.

Key supporting areas:

- page components under `frontend/src/components`
- request logic in `frontend/src/api.ts`
- auth/token handling in `frontend/src/auth.ts`
- shared validation in `frontend/src/lib/validation.ts`
- shared formatting in `frontend/src/lib/format.ts`

This keeps rendering, validation, and data-fetching concerns more clearly separated than the earlier single-dashboard version.
