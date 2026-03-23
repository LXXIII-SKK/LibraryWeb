# Circulation Flow

## Purpose

This module manages borrowing, returning, renewing, reservations, fines, and policy-driven circulation operations for both members and staff.

## Actors

- Authenticated member
- Branch manager
- Admin
- Auditor

## Main Components

### Frontend

- books workspace borrow/reserve actions
- book detail borrow/reserve actions
- `/me` borrowings, reservations, and fines panels
- operations workspace circulation panels

### Backend

- `BorrowController`
- `ReservationController`
- `FineController`
- `PolicyController`
- `CirculationService`
- `ReservationService`
- `FineService`
- `PolicyService`

## Main Endpoints

- `POST /api/borrowings`
- `POST /api/borrowings/staff-checkout`
- `POST /api/borrowings/{transactionId}/return`
- `POST /api/borrowings/{transactionId}/renew`
- `GET /api/borrowings/me`
- `GET /api/borrowings`
- `POST /api/reservations`
- `GET /api/reservations/me`
- `GET /api/reservations`
- `POST /api/reservations/{reservationId}/cancel`
- `POST /api/reservations/{reservationId}/prepare`
- `POST /api/reservations/{reservationId}/ready`
- `POST /api/reservations/{reservationId}/expire`
- `POST /api/reservations/{reservationId}/collect`
- `POST /api/reservations/{reservationId}/no-show`
- `GET /api/fines/me`
- `GET /api/fines`
- `POST /api/fines/{fineId}/waive`
- `GET /api/policies/current`
- `PUT /api/policies/current`

## 1. Self-Service Borrow Flow

1. An authenticated member selects a title and, when needed, a concrete holding.
2. The frontend submits `POST /api/borrowings`.
3. `AuthorizationService` requires:
   - role `MEMBER`
   - `ACTIVE` account status
   - `GOOD_STANDING` membership status
4. `CirculationService` checks availability and creates the transaction.
5. Due dates are generated from the active library policy; the member does not choose an arbitrary due date.
6. A borrow event is published and the frontend refreshes books, borrowings, fines, reservations, and activity panels as needed.

## 2. Return And Renew Flow

1. From `/me`, the member can return or renew eligible borrowings.
2. `POST /api/borrowings/{transactionId}/return` allows:
   - the owning member on their own borrowing
   - branch-scoped staff with return authority inside branch scope
   - admins globally
3. `POST /api/borrowings/{transactionId}/renew` allows:
   - the owning member only when still `GOOD_STANDING`
   - branch-scoped staff with override authority inside branch scope
   - admins globally
4. The transaction is updated and an audit event is recorded.

## 3. Reservation Flow

1. A member in `ACTIVE` + `GOOD_STANDING` creates a reservation through `POST /api/reservations`.
2. `/me` loads the user's reservation list through `GET /api/reservations/me`.
3. The member can cancel eligible reservations and collect `READY_FOR_PICKUP` reservations.
4. Operational staff can review and manage all visible reservations:
   - prepare a reservation for pickup
   - mark it ready
   - expire it
   - mark it no-show
5. The frontend reflects these state changes in both `/me` and `/admin`.

## 4. Operational Circulation Flow

1. Authorized staff open the operations workspace.
2. Depending on permission scope, the frontend calls `GET /api/borrowings`, `GET /api/reservations`, and `GET /api/fines`.
3. Branch managers get branch-scoped operational control.
4. Admins get global operational control.
5. Auditors get global read-only visibility.
6. Staff checkout for a member uses `POST /api/borrowings/staff-checkout`.
7. Fine waivers use `POST /api/fines/{fineId}/waive` and still respect branch limits and scope rules.

## Business Rules

- Self-service borrow, reserve, renew, and collect require `ACTIVE` + `GOOD_STANDING`.
- Members in restricted membership states can still inspect their account data if authenticated, but they cannot start new borrowing-style actions.
- Staff operational actions are branch-scoped unless the caller is `ADMIN`.
- Auditors can review operational records but cannot mutate them.

## Affected Tables

- `borrow_transaction`
- `reservation`
- `fine_record`
- `library_policy`
- `book`
- `book_holding`
- `activity_log`
- `event_publication`
