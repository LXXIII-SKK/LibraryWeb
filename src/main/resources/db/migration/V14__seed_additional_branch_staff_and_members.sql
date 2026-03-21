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
    (9, 'seed-east-librarian', 'east.librarian', 'east.librarian@library.local', 'LIBRARIAN', 'ACTIVE', 'GOOD_STANDING', 2, 2, current_timestamp, current_timestamp),
    (10, 'seed-east-manager', 'east.manager', 'east.manager@library.local', 'BRANCH_MANAGER', 'ACTIVE', 'GOOD_STANDING', 2, 2, current_timestamp, current_timestamp),
    (11, 'seed-hq-librarian', 'hq.librarian', 'hq.librarian@library.local', 'LIBRARIAN', 'ACTIVE', 'GOOD_STANDING', 999, 999, current_timestamp, current_timestamp),
    (12, 'seed-hq-manager', 'hq.manager', 'hq.manager@library.local', 'BRANCH_MANAGER', 'ACTIVE', 'GOOD_STANDING', 999, 999, current_timestamp, current_timestamp),
    (13, 'seed-central-member', 'central.member', 'central.member@library.local', 'MEMBER', 'ACTIVE', 'GOOD_STANDING', 1, 1, current_timestamp, current_timestamp),
    (14, 'seed-east-member', 'east.member', 'east.member@library.local', 'MEMBER', 'ACTIVE', 'GOOD_STANDING', 2, 2, current_timestamp, current_timestamp),
    (15, 'seed-hq-member', 'hq.member', 'hq.member@library.local', 'MEMBER', 'ACTIVE', 'GOOD_STANDING', 999, 999, current_timestamp, current_timestamp);

select setval('app_user_id_seq', (select max(id) from app_user));
