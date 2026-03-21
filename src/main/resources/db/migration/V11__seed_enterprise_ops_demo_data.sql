insert into book_tag (book_id, name)
values
    (1, 'ddd'),
    (1, 'domain'),
    (1, 'strategy'),
    (2, 'data'),
    (2, 'distributed-systems'),
    (2, 'streams'),
    (3, 'architecture'),
    (3, 'boundaries'),
    (3, 'policy'),
    (4, 'legacy'),
    (4, 'refactoring'),
    (4, 'testing'),
    (5, 'devops'),
    (5, 'delivery'),
    (5, 'metrics'),
    (6, 'leadership'),
    (6, 'team-design'),
    (6, 'platform-teams'),
    (7, 'architecture'),
    (7, 'fitness-functions'),
    (7, 'evolution'),
    (8, 'operations'),
    (8, 'resilience'),
    (8, 'production');

insert into library_policy (
    id,
    standard_loan_days,
    renewal_days,
    max_renewals,
    fine_per_overdue_day,
    fine_waiver_limit,
    allow_renewal_with_active_reservations,
    updated_at
)
values
    (1, 14, 7, 2, 1.50, 10.00, false, current_timestamp);

update borrow_transaction
set renewal_count = 1,
    last_renewed_at = current_timestamp - interval '2 days'
where id = 1;

insert into reservation (id, user_id, book_id, reserved_at, updated_at, status)
values
    (1, 2, 2, current_timestamp - interval '1 day', current_timestamp - interval '1 day', 'ACTIVE'),
    (2, 3, 5, current_timestamp - interval '3 days', current_timestamp - interval '3 days', 'ACTIVE'),
    (3, 4, 6, current_timestamp - interval '10 days', current_timestamp - interval '9 days', 'NO_SHOW');

insert into fine_record (
    id,
    user_id,
    borrow_transaction_id,
    amount,
    reason,
    status,
    created_at,
    resolved_at,
    resolved_by_user_id,
    resolution_note
)
values
    (1, 4, 2, 4.50, 'Overdue borrowing requires manual follow-up', 'OPEN', current_timestamp - interval '1 day', null, null, null),
    (2, 5, 3, 3.00, 'Returned 2 days late', 'WAIVED', current_timestamp - interval '5 days', current_timestamp - interval '4 days', 7, 'Manager-approved courtesy waiver');

select setval('book_tag_id_seq', (select max(id) from book_tag));
select setval('reservation_id_seq', (select max(id) from reservation));
select setval('fine_record_id_seq', (select max(id) from fine_record));
