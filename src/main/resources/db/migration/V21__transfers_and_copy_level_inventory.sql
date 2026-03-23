create table book_copy (
    id bigserial primary key,
    holding_id bigint not null references book_holding(id) on delete cascade,
    barcode varchar(80) not null,
    status varchar(30) not null,
    current_branch_id bigint not null references library_branch(id),
    current_location_id bigint references library_location(id),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uq_book_copy_barcode unique (barcode)
);

alter table borrow_transaction
    add column copy_id bigint references book_copy(id);

create table book_transfer (
    id bigserial primary key,
    copy_id bigint not null references book_copy(id),
    source_holding_id bigint not null references book_holding(id),
    destination_branch_id bigint not null references library_branch(id),
    status varchar(30) not null,
    requested_at timestamp with time zone not null,
    dispatched_at timestamp with time zone not null,
    ready_at timestamp with time zone,
    completed_at timestamp with time zone,
    closed_at timestamp with time zone
);

alter table reservation
    add column reserved_copy_id bigint references book_copy(id),
    add column transfer_id bigint references book_transfer(id);

create index idx_book_copy_holding on book_copy(holding_id);
create index idx_book_copy_current_branch on book_copy(current_branch_id);
create index idx_book_copy_status on book_copy(status);
create index idx_borrow_transaction_copy on borrow_transaction(copy_id);
create index idx_book_transfer_source_holding on book_transfer(source_holding_id);
create index idx_book_transfer_destination_branch on book_transfer(destination_branch_id);
create index idx_book_transfer_status on book_transfer(status);
create index idx_reservation_reserved_copy on reservation(reserved_copy_id);
create index idx_reservation_transfer on reservation(transfer_id);

insert into book_copy (holding_id, barcode, status, current_branch_id, current_location_id, created_at, updated_at)
select
    h.id,
    format('H%s-C%s', h.id, sequence_number),
    case
        when sequence_number <= h.available_quantity then 'AVAILABLE'
        else 'UNAVAILABLE'
    end,
    h.branch_id,
    h.location_id,
    current_timestamp,
    current_timestamp
from book_holding h
join lateral generate_series(1, h.total_quantity) sequence_number on true
where h.format = 'PHYSICAL';

select setval('book_copy_id_seq', (select coalesce(max(id), 1) from book_copy));
select setval('book_transfer_id_seq', (select coalesce(max(id), 1) from book_transfer));
