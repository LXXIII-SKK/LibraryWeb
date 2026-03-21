# Project Flows Index

This folder documents how each module and major website function works end-to-end.

## Files

- `01-system-overview.md`
  - high-level request flow and module responsibilities
- `02-identity-and-access-flow.md`
  - login, registration, role handling, and profile synchronization
- `03-catalog-flow.md`
  - catalog search, book detail lookup, and admin catalog operations
- `04-circulation-flow.md`
  - borrowing, returning, user history, and admin oversight
- `05-activity-log-flow.md`
  - audit logging, view tracking, and activity feeds
- `06-frontend-web-flows.md`
  - current page structure and user-facing flows across the web app

## Current Web Structure

- `/`
  - welcome and discovery page
- `/books`
  - books workspace
- `/books/:id`
  - book detail page
- `/me`
  - user account page
- `/admin`
  - admin-only management page

## How To Read These Docs

Each document follows the same pattern:

- purpose
- actors
- main endpoints
- step-by-step flow
- business rules
- affected tables

Use these files as the functional documentation for the web application.
