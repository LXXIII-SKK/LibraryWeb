# Catalog Flow

## Purpose

This module manages book search and admin catalog maintenance.

## Actors

- Guest
- Authenticated user
- Admin

## Main Components

### Frontend

- `CatalogPanel`
- `AdminConsole`
- `api.ts`

### Backend

- `BookController`
- `CatalogService`
- `BookRepository`
- `Book`

## Main Endpoints

- `GET /api/books`
- `POST /api/books`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`

## Search Flow

1. User enters a search term or category filter in the catalog screen.
2. Frontend calls `GET /api/books`.
3. Query parameters may include:
   - `query`
   - `category`
4. `BookController.search()` receives the request.
5. `CatalogService.search()` normalizes empty filters.
6. `BookRepository.search()` executes the JPQL query.
7. Matching books are returned as `BookResponse`.
8. Frontend renders catalog cards with:
   - title
   - author
   - category
   - ISBN
   - available quantity
   - total quantity

## Create Book Flow

1. Admin opens the Admin Console.
2. Admin fills in the book form.
3. Frontend sends `POST /api/books`.
4. `BookController.create()` requires admin role.
5. `CatalogService.create()` trims and normalizes input.
6. A new `Book` entity is created and saved.
7. Frontend reloads the catalog list.

## Update Book Flow

1. Admin clicks `Edit` on a catalog item.
2. Book data is loaded into the admin form.
3. Frontend sends `PUT /api/books/{id}`.
4. `CatalogService.update()` loads the existing book.
5. `Book.update()` validates inventory consistency.
6. The updated book is persisted.
7. Frontend reloads current data.

## Delete Book Flow

1. Admin clicks `Delete`.
2. Frontend asks for confirmation.
3. Frontend sends `DELETE /api/books/{id}`.
4. `CatalogService.delete()` removes the book.
5. Frontend refreshes the catalog.

## Business Rules

- Guests can browse the catalog.
- Only admins can create, update, or delete books.
- `totalQuantity` cannot be lower than the number of already borrowed copies.
- Inventory values are maintained inside the entity, not only in the UI.

## Affected Tables

- `book`
