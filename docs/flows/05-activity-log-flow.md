# Activity Log Flow

## Purpose

This module records and exposes audit history for borrow and return actions.

## Actors

- Authenticated user
- Admin
- Event publisher

## Main Components

### Backend

- `ActivityLogService`
- `ActivityLogController`
- `ActivityLogRepository`
- `BookBorrowedEvent`
- `BookReturnedEvent`

### Frontend

- activity panel
- admin audit feed

## Main Endpoints

- `GET /api/activity-logs/me`
- `GET /api/activity-logs`

## Borrow Event Logging Flow

1. A user borrows a book.
2. `CirculationService` publishes `BookBorrowedEvent`.
3. `ActivityLogService.onBookBorrowed()` listens to the event.
4. It resolves references to:
   - user
   - book
5. It creates an `ActivityLog` record with:
   - activity type `BORROWED`
   - message text
   - occurrence time
6. The record is saved.

## Return Event Logging Flow

1. A user or admin returns a book.
2. `CirculationService` publishes `BookReturnedEvent`.
3. `ActivityLogService.onBookReturned()` listens to the event.
4. It creates an `ActivityLog` record with:
   - activity type `RETURNED`
   - message text
   - occurrence time
5. The record is saved.

## Current User Activity Flow

1. Frontend calls `GET /api/activity-logs/me`.
2. `ActivityLogController.myActivityLogs()` delegates to the service.
3. The service resolves the current user.
4. Logs are loaded by user id, newest first.
5. Frontend shows the user activity timeline.

## Admin Audit Flow

1. Admin opens the activity panel.
2. Frontend calls `GET /api/activity-logs`.
3. Endpoint is restricted to admin.
4. All logs are loaded newest first.
5. Frontend shows the global audit feed.

## Business Rules

- Activity logs are generated automatically from circulation events.
- Normal users can only view their own activity history.
- Admins can view all activity logs.

## Affected Tables

- `activity_log`
- `event_publication`
- `event_publication_archive`
