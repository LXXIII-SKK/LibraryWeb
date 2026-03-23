# Project Flows Index

This folder documents how the current backend modules and browser routes behave end-to-end.

## Files

- `01-system-overview.md`
  - high-level request flow, security entry points, and module responsibilities
- `02-identity-and-access-flow.md`
  - login, seeded-user relinking, member bootstrap rules, access management, and discipline workflows
- `03-catalog-flow.md`
  - public catalog browsing, filtering, detail lookup, and staff catalog maintenance
- `04-circulation-flow.md`
  - borrowing, renewals, reservations, fines, policy-driven due dates, and operational circulation review
- `05-activity-log-flow.md`
  - audit/event logging, personal history, and branch/global operational review
- `06-frontend-web-flows.md`
  - current page structure and route-level behavior across the React app

## Current Web Structure

- `/`
  - welcome and discovery page
- `/books`
  - books workspace
- `/upcoming`
  - upcoming acquisitions workspace
- `/books/:id`
  - book detail page
- `/me`
  - member and account workspace
- `/admin`
  - permission-scoped operations workspace

## How To Read These Docs

Each document follows the same pattern:

- purpose
- actors
- main endpoints or screens
- step-by-step flow
- business rules
- affected tables or modules

Use these files as the detailed functional reference for the shipped application behavior.
