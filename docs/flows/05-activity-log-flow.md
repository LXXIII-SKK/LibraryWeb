# Activity Log Flow

## Purpose

This module records and exposes personal history plus operational audit history across circulation, policy, and user-access changes.

## Actors

- Authenticated member
- Branch manager
- Admin
- Auditor
- Event publishers

## Main Components

### Backend

- `ActivityLogService`
- `ActivityLogController`
- `ActivityLogRepository`
- circulation, reservation, fine, policy, and identity domain events

### Frontend

- `/me` activity panel
- operations workspace activity panel
- book detail recent activity context

## Main Endpoints

- `POST /api/books/{id}/view`
- `GET /api/activity-logs/me`
- `GET /api/activity-logs`

## Logged Event Sources

The current application records activity for:

- book views
- borrowing
- returns
- renewals
- reservation create and cancel
- reservation no-show
- fine waivers
- policy updates
- access updates
- access discipline actions

## Book View Count Flow

1. An authenticated non-auditor opens `/books/:id`.
2. The frontend calls `POST /api/books/{id}/view`.
3. `ActivityLogService` checks whether the current user already has a `VIEWED` row for that book.
4. If not, it writes the row and returns `{ bookId, viewCount, counted: true }`.
5. If the user already viewed that book in the current cycle, the backend returns the unchanged count with `counted: false`.
6. The frontend uses that returned count directly instead of incrementing locally.

Reset rule:

- the weekly reset script deletes `VIEWED` rows from `activity_log`
- that both resets the discovery view totals and re-enables one new counted view per user and book

## Personal History Flow

1. An authenticated user opens `/me`.
2. The frontend calls `GET /api/activity-logs/me` when the role has self-history access.
3. `ActivityLogService` resolves the current user and loads records newest first.
4. The page renders the personal activity feed beside profile, borrowings, reservations, fines, and notifications.

## Operational Audit Flow

1. A user with audit visibility opens the operations workspace.
2. The frontend calls `GET /api/activity-logs`.
3. `AuthorizationService.canReadAuditLogs()` allows:
   - branch managers with branch reporting permissions and a branch assignment
   - admins globally
   - auditors globally
4. The backend returns the visible operational activity feed newest first.

## Business Rules

- Activity rows are produced automatically from domain events or explicit book-view recording.
- Book views are deduplicated per user and book until the next reset period.
- Members only see their own history.
- Branch managers get operational audit visibility in branch scope.
- Admins and auditors get global operational audit visibility.

## Affected Tables

- `activity_log`
- `event_publication`
- `event_publication_archive`
