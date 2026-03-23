# System Overview Flow

## Purpose

This document summarizes the current request flow across the Mini Library stack and names the modules that own the main responsibilities.

## Main Layers

### Frontend

- React 19 application in `frontend/src`
- route handling, Keycloak session bootstrap, API orchestration, and permission-driven page composition

### API Layer

- Spring REST controllers under `src/main/java/com/example/library`
- method-level authorization through `AuthorizationService`

### Business Layer

- services such as `CurrentUserService`, `CatalogService`, `CirculationService`, `AccessManagementService`, and `NotificationService`
- applies identity synchronization, scope checks, and library business rules

### Persistence Layer

- Spring Data JPA repositories
- Flyway-managed PostgreSQL schema and seed data

## High-Level Request Flow

1. The browser loads the React application.
2. The frontend performs a Keycloak silent SSO check.
3. Public pages load discovery, books, filters, upcoming books, and public branches.
4. After authentication, the frontend calls protected APIs such as `/api/profile`.
5. Spring Security validates the JWT and resolves realm roles into authorities.
6. `CurrentUserService` synchronizes the current principal into `app_user`.
7. Controllers delegate to services, which enforce role, account-status, membership-status, branch-scope, and global-scope rules.
8. Repositories read or write PostgreSQL entities.
9. Domain events publish follow-up audit entries where required.
10. The frontend refreshes the relevant panels and lists.

In single-origin public-test mode:

- the frontend is still the only public entry point
- nginx proxies `/api` to the backend and `/auth` to Keycloak behind that same origin

## Identity Entry Point

The first protected request is usually `GET /api/profile`.

At that point:

- seeded demo users normally match directly by their deterministic Keycloak subject id
- legacy `seed-*` placeholder identities can still be rebound once by username as a compatibility path
- a new `MEMBER` identity can be auto-bootstrapped into `app_user`
- non-member identities without local provisioning are rejected

## Main Modules

- `identity`
  - current-user synchronization, profile, permissions, access management, discipline
- `catalog`
  - books, tags, covers, filter metadata
- `circulation`
  - borrowings, renewals, reservations, fines, policies
- `history`
  - personal history and operational audit feeds
- `branch`
  - branch directory and summaries
- `inventory`
  - locations, physical holdings, digital access
- `notification`
  - staff notifications and librarian discipline-review requests
- `discovery`
  - landing-page recommendations and rankings
- `upcoming`
  - upcoming acquisitions

## Main Tables

- `app_user`
- `user_discipline_record`
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
- `staff_notification`
- `staff_notification_receipt`
- `upcoming_book`
- `event_publication`
