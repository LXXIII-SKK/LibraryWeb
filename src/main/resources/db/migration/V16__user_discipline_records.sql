create table user_discipline_record (
    id bigserial primary key,
    target_user_id bigint not null references app_user(id) on delete cascade,
    actor_user_id bigint not null references app_user(id),
    action_type varchar(30) not null,
    reason_code varchar(50) not null,
    note varchar(500),
    previous_account_status varchar(30) not null,
    resulting_account_status varchar(30) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create index idx_user_discipline_record_target_created
    on user_discipline_record(target_user_id, created_at desc);

create index idx_user_discipline_record_actor_created
    on user_discipline_record(actor_user_id, created_at desc);
