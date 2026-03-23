alter table borrow_transaction
    add column last_renewal_override boolean not null default false,
    add column last_renewal_reason varchar(255),
    add column exception_note varchar(500),
    add column exception_recorded_at timestamp with time zone;
