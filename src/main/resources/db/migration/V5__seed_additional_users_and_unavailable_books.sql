insert into book (id, title, author, category, isbn, total_quantity, available_quantity, created_at, updated_at)
values
    (9, 'The Phoenix Project', 'Gene Kim', 'DevOps', '9781942788294', 2, 0, current_timestamp, current_timestamp),
    (10, 'Site Reliability Engineering', 'Betsy Beyer', 'Operations', '9781491929124', 1, 0, current_timestamp, current_timestamp),
    (11, 'Working Effectively with Legacy Code', 'Michael Feathers', 'Engineering', '9780131177055', 3, 1, current_timestamp, current_timestamp);

insert into app_user (id, keycloak_user_id, username, email, role, created_at, updated_at)
values
    (101, 'seed-sophia-chen', 'sophia.chen', 'sophia.chen@library.local', 'USER', current_timestamp, current_timestamp),
    (102, 'seed-daniel-lee', 'daniel.lee', 'daniel.lee@library.local', 'USER', current_timestamp, current_timestamp),
    (103, 'seed-linh-pham', 'linh.pham', 'linh.pham@library.local', 'USER', current_timestamp, current_timestamp),
    (104, 'seed-ops-admin', 'ops.admin', 'ops.admin@library.local', 'ADMIN', current_timestamp, current_timestamp);

insert into borrow_transaction (id, user_id, book_id, borrowed_at, due_at, returned_at, status)
values
    (101, 101, 9, current_timestamp - interval '4 days', current_timestamp + interval '10 days', null, 'BORROWED'),
    (102, 102, 9, current_timestamp - interval '2 days', current_timestamp + interval '12 days', null, 'BORROWED'),
    (103, 103, 10, current_timestamp - interval '1 day', current_timestamp + interval '13 days', null, 'BORROWED'),
    (104, 101, 11, current_timestamp - interval '8 days', current_timestamp + interval '4 days', null, 'BORROWED'),
    (105, 104, 3, current_timestamp - interval '5 days', current_timestamp + interval '9 days', null, 'BORROWED');

insert into activity_log (id, user_id, book_id, activity_type, message, occurred_at)
values
    (101, 101, 9, 'BORROWED', 'sophia.chen borrowed "The Phoenix Project"', current_timestamp - interval '4 days'),
    (102, 102, 9, 'BORROWED', 'daniel.lee borrowed "The Phoenix Project"', current_timestamp - interval '2 days'),
    (103, 103, 10, 'BORROWED', 'linh.pham borrowed "Site Reliability Engineering"', current_timestamp - interval '1 day'),
    (104, 101, 11, 'BORROWED', 'sophia.chen borrowed "Working Effectively with Legacy Code"', current_timestamp - interval '8 days'),
    (105, 104, 3, 'BORROWED', 'ops.admin borrowed "Clean Architecture"', current_timestamp - interval '5 days');

select setval('book_id_seq', (select max(id) from book));
select setval('app_user_id_seq', (select max(id) from app_user));
select setval('borrow_transaction_id_seq', (select max(id) from borrow_transaction));
select setval('activity_log_id_seq', (select max(id) from activity_log));
