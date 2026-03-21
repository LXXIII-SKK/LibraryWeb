# System Overview Flow

## Purpose

This document explains the overall request structure of the Mini Library Management System.

## Main Layers

### Presentation Layer

Responsible for user interaction and HTTP request handling.

- React frontend
- Spring REST controllers

Examples:

- `frontend/src/App.tsx`
- `src/main/java/com/example/library/catalog/BookController.java`
- `src/main/java/com/example/library/circulation/BorrowController.java`

### Business Logic Layer

Responsible for applying business rules and coordinating operations.

Examples:

- `CatalogService`
- `CirculationService`
- `ActivityLogService`
- `CurrentUserService`

### Data Access Layer

Responsible for database reads and writes through Spring Data JPA.

Examples:

- `BookRepository`
- `BorrowTransactionRepository`
- `ActivityLogRepository`
- `AppUserRepository`

## High-Level Request Flow

1. User interacts with the React web UI.
2. Frontend sends an HTTP request to the backend API.
3. Spring controller receives the request.
4. Controller calls the service layer.
5. Service executes business rules.
6. Service uses repositories to access PostgreSQL.
7. Service returns DTOs to the controller.
8. Controller returns JSON to the frontend.
9. Frontend renders the updated state.

## Example

Borrowing a book:

1. User clicks `Borrow` in the catalog UI.
2. Frontend calls `POST /api/borrowings`.
3. `BorrowController` receives the request.
4. `CirculationService` validates current user and book stock.
5. `Book.borrowOne()` decreases available quantity.
6. `BorrowTransactionRepository` saves the transaction.
7. A domain event is published.
8. `ActivityLogService` listens to the event and records the activity.
9. Frontend reloads borrowings, books, and activity history.

## Main Modules

- Identity
  - user synchronization from Keycloak
  - role resolution
- Catalog
  - search and book management
- Circulation
  - borrow and return lifecycle
- History
  - audit logging

## Main Tables

- `app_user`
- `book`
- `borrow_transaction`
- `activity_log`
- `event_publication`
- `event_publication_archive`
