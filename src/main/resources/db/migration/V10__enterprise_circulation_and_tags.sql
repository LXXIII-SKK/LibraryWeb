alter table borrow_transaction
    add column renewal_count int not null default 0,
    add column last_renewed_at timestamp null;

create table book_tag (
    id bigserial primary key,
    book_id bigint not null references book(id) on delete cascade,
    name varchar(50) not null,
    constraint uq_book_tag unique (book_id, name)
);

create index idx_book_tag_name on book_tag(name);

create table reservation (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    book_id bigint not null references book(id),
    reserved_at timestamp not null,
    updated_at timestamp not null,
    status varchar(30) not null
);

create index idx_reservation_user_status on reservation(user_id, status);
create index idx_reservation_book_status on reservation(book_id, status);

create table library_policy (
    id bigint primary key,
    standard_loan_days int not null,
    renewal_days int not null,
    max_renewals int not null,
    fine_per_overdue_day numeric(10, 2) not null,
    fine_waiver_limit numeric(10, 2) not null,
    allow_renewal_with_active_reservations boolean not null default false,
    updated_at timestamp not null default current_timestamp
);

create table fine_record (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    borrow_transaction_id bigint references borrow_transaction(id),
    amount numeric(10, 2) not null,
    reason varchar(255) not null,
    status varchar(30) not null,
    created_at timestamp not null,
    resolved_at timestamp null,
    resolved_by_user_id bigint references app_user(id),
    resolution_note varchar(255)
);

create index idx_fine_record_user_status on fine_record(user_id, status);
create index idx_fine_record_created_at on fine_record(created_at desc);
