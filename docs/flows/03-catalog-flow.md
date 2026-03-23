# Catalog Flow

## Purpose

This module manages public catalog browsing plus staff catalog maintenance for books, tags, and cover images.

## Actors

- Guest
- Authenticated member
- Librarian
- Branch manager
- Admin
- Auditor

## Main Components

### Frontend

- `BooksWorkspacePage`
- `BookDetailPage`
- `CatalogPanel`
- `BookCoverArt`

### Backend

- `BookController`
- `CatalogService`
- `BookRepository`
- `BookCoverRepository`
- `Book`

## Main Endpoints

- `GET /api/books`
- `GET /api/books/filters`
- `GET /api/books/{id}`
- `GET /api/books/{id}/cover`
- `POST /api/books`
- `PUT /api/books/{id}`
- `POST /api/books/{id}/cover`
- `DELETE /api/books/{id}`

## Search And Filter Flow

1. The browser loads the books workspace or the landing page.
2. The frontend calls `GET /api/books` and `GET /api/books/filters`.
3. Query parameters can include:
   - `query`
   - `category`
   - `tag`
4. `CatalogService` normalizes empty filters and searches the catalog.
5. Results return with tags, current availability totals, online-access flags, and cover metadata.
6. The frontend renders the catalog in 4-card pages and lets the user open `/books/:id`.

## Book Detail Flow

1. The user opens a specific title.
2. The frontend calls `GET /api/books/{id}`.
3. If a cover exists, the UI requests `GET /api/books/{id}/cover`.
4. The response includes branch-aware holdings and digital availability details.
5. Signed-in non-auditors attempt `POST /api/books/{id}/view`.
6. `ActivityLogService` counts at most one view per authenticated user and book until the next reset cycle and returns the authoritative count.
7. Signed-in users can continue into borrowing or reservation flows from this page if their account state permits it.

## Upcoming Workspace Flow

1. The user opens `/upcoming`.
2. The frontend loads `GET /api/upcoming-books`.
3. Client-side search and branch filters narrow the result set.
4. The page renders upcoming titles in 4-card pages with expected arrival dates, branch context, and tags.

## Create Or Update Flow

1. A staff user opens the operations workspace.
2. The catalog panel is visible only when the caller has `BOOK_CREATE` or `BOOK_UPDATE`.
3. The frontend submits `POST /api/books` or `PUT /api/books/{id}`.
4. Authorized roles are active librarians, branch managers, and admins.
5. `CatalogService` validates the request and persists the book.
6. Optional cover upload is handled separately through `POST /api/books/{id}/cover`.
7. The frontend refreshes the catalog and the operations panel state.

## Delete Flow

1. An admin chooses `Delete` in the operations workspace.
2. The frontend confirms the action.
3. The frontend submits `DELETE /api/books/{id}`.
4. Only `ADMIN` is allowed to hard-delete a book.
5. The catalog data is refreshed after success.

## Business Rules

- Public reads are anonymous.
- Catalog create and update require an active, non-read-only operational role with book permissions.
- Book deletion is admin-only.
- Catalog availability is derived from current holdings and transactions, not just from client-side state.

## Affected Tables

- `book`
- `book_tag`
- `book_cover`
- `book_holding`
