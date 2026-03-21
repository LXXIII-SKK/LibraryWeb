truncate table activity_log, borrow_transaction, app_user, book restart identity cascade;

insert into book (id, title, author, category, isbn, total_quantity, available_quantity, created_at, updated_at, version)
values
    (1, 'Domain-Driven Design', 'Eric Evans', 'Architecture', '9780321125217', 6, 4, current_timestamp, current_timestamp, 0),
    (2, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'Architecture', '9781449373320', 5, 3, current_timestamp, current_timestamp, 0),
    (3, 'Clean Architecture', 'Robert C. Martin', 'Architecture', '9780134494166', 4, 4, current_timestamp, current_timestamp, 0),
    (4, 'Refactoring', 'Martin Fowler', 'Engineering', '9780134757599', 7, 6, current_timestamp, current_timestamp, 0),
    (5, 'Accelerate', 'Nicole Forsgren', 'DevOps', '9781942788331', 5, 5, current_timestamp, current_timestamp, 0),
    (6, 'Team Topologies', 'Matthew Skelton', 'Leadership', '9781942788812', 3, 3, current_timestamp, current_timestamp, 0),
    (7, 'Building Evolutionary Architectures', 'Neal Ford', 'Architecture', '9781491986363', 4, 4, current_timestamp, current_timestamp, 0),
    (8, 'Release It!', 'Michael T. Nygard', 'Operations', '9781680504552', 4, 3, current_timestamp, current_timestamp, 0);

insert into app_user (
    id,
    keycloak_user_id,
    username,
    email,
    role,
    account_status,
    membership_status,
    branch_id,
    home_branch_id,
    created_at,
    updated_at
)
values
    (1, 'seed-admin', 'admin', 'admin@library.local', 'ADMIN', 'ACTIVE', 'GOOD_STANDING', 999, 999, current_timestamp, current_timestamp),
    (2, 'seed-reader', 'reader', 'reader@library.local', 'MEMBER', 'ACTIVE', 'GOOD_STANDING', 1, 1, current_timestamp, current_timestamp),
    (3, 'seed-alina-reader', 'alina.reader', 'alina.reader@library.local', 'MEMBER', 'ACTIVE', 'GOOD_STANDING', 1, 1, current_timestamp, current_timestamp),
    (4, 'seed-hoang-nguyen', 'hoang.nguyen', 'hoang.nguyen@library.local', 'MEMBER', 'ACTIVE', 'OVERDUE_RESTRICTED', 1, 1, current_timestamp, current_timestamp),
    (5, 'seed-maya-tran', 'maya.tran', 'maya.tran@library.local', 'MEMBER', 'ACTIVE', 'BORROW_BLOCKED', 2, 2, current_timestamp, current_timestamp),
    (6, 'seed-branch-librarian', 'branch.librarian', 'branch.librarian@library.local', 'LIBRARIAN', 'ACTIVE', 'GOOD_STANDING', 1, 1, current_timestamp, current_timestamp),
    (7, 'seed-branch-manager', 'branch.manager', 'branch.manager@library.local', 'BRANCH_MANAGER', 'ACTIVE', 'GOOD_STANDING', 1, 1, current_timestamp, current_timestamp),
    (8, 'seed-compliance-auditor', 'compliance.auditor', 'compliance.auditor@library.local', 'AUDITOR', 'ACTIVE', 'GOOD_STANDING', null, null, current_timestamp, current_timestamp);

insert into borrow_transaction (id, user_id, book_id, borrowed_at, due_at, returned_at, status)
values
    (1, 3, 1, current_timestamp - interval '6 days', current_timestamp + interval '8 days', null, 'BORROWED'),
    (2, 4, 2, current_timestamp - interval '12 days', current_timestamp + interval '2 days', null, 'BORROWED'),
    (3, 5, 4, current_timestamp - interval '20 days', current_timestamp - interval '6 days', current_timestamp - interval '3 days', 'RETURNED'),
    (4, 2, 8, current_timestamp - interval '2 days', current_timestamp + interval '12 days', null, 'BORROWED');

insert into activity_log (id, user_id, book_id, activity_type, message, occurred_at)
values
    (1, 3, 1, 'BORROWED', 'alina.reader borrowed "Domain-Driven Design"', current_timestamp - interval '6 days'),
    (2, 4, 2, 'BORROWED', 'hoang.nguyen borrowed "Designing Data-Intensive Applications"', current_timestamp - interval '12 days'),
    (3, 5, 4, 'BORROWED', 'maya.tran borrowed "Refactoring"', current_timestamp - interval '20 days'),
    (4, 5, 4, 'RETURNED', 'maya.tran returned "Refactoring"', current_timestamp - interval '3 days'),
    (5, 2, 8, 'BORROWED', 'reader borrowed "Release It!"', current_timestamp - interval '2 days'),
    (6, 2, 2, 'VIEWED', 'reader viewed "Designing Data-Intensive Applications"', current_timestamp - interval '1 day'),
    (7, 3, 1, 'VIEWED', 'alina.reader viewed "Domain-Driven Design"', current_timestamp - interval '2 days'),
    (8, 4, 6, 'VIEWED', 'hoang.nguyen viewed "Team Topologies"', current_timestamp - interval '3 hours');

select setval('book_id_seq', (select max(id) from book));
select setval('app_user_id_seq', (select max(id) from app_user));
select setval('borrow_transaction_id_seq', (select max(id) from borrow_transaction));
select setval('activity_log_id_seq', (select max(id) from activity_log));
