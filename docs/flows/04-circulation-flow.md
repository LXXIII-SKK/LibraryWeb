# Circulation Flow

## Purpose

This module manages borrowing, returning, user borrowing history, and admin transaction oversight.

## Actors

- Authenticated user
- Admin

## Main Components

### Frontend

- catalog borrow action
- my borrowings panel
- admin borrower oversight table

### Backend

- `BorrowController`
- `CirculationService`
- `BorrowTransaction`
- `BorrowTransactionRepository`
- `Book`

## Main Endpoints

- `POST /api/borrowings`
- `POST /api/borrowings/{transactionId}/return`
- `GET /api/borrowings/me`
- `GET /api/borrowings`

## Borrow Book Flow

1. Signed-in user chooses a book and due date.
2. Frontend sends `POST /api/borrowings`.
3. `BorrowController.borrow()` forwards the request to `CirculationService`.
4. `CirculationService.borrow()` resolves the current user.
5. `CatalogService.findEntity()` loads the target book.
6. `Book.borrowOne()` checks stock and decreases available quantity.
7. A new `BorrowTransaction` is created.
8. The transaction is saved.
9. A `BookBorrowedEvent` is published.
10. Frontend refreshes books, borrowings, and activity logs.

## Return Book Flow

1. User clicks `Return` on one of their borrowings.
2. Frontend sends `POST /api/borrowings/{transactionId}/return`.
3. `CirculationService.returnBook()` loads the transaction.
4. It checks:
   - transaction exists
   - transaction belongs to the user or caller is admin
   - transaction is not already returned
5. `Book.returnOne()` increases available quantity.
6. Transaction is marked returned with `returnedAt`.
7. A `BookReturnedEvent` is published.
8. Frontend refreshes data.

## My Borrowings Flow

1. Frontend calls `GET /api/borrowings/me`.
2. `CirculationService.listForCurrentUser()` resolves the user.
3. Transactions are loaded in descending borrow date order.
4. Frontend shows current and past loans.

## Admin Oversight Flow

1. Admin opens the Admin Console.
2. Frontend calls `GET /api/borrowings`.
3. `BorrowController.allBorrowings()` is protected by admin authorization.
4. All transactions are returned.
5. Admin can inspect user, book, due date, and status.
6. Admin can force return a still-borrowed item.

## Business Rules

- Only authenticated users can borrow or return.
- Stock must exist before borrowing.
- Non-admin users can return only their own books.
- Already returned transactions cannot be returned again.

## Affected Tables

- `book`
- `borrow_transaction`
- `event_publication`
- `event_publication_archive`
