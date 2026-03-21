create table app_user (
    id bigserial primary key,
    keycloak_user_id varchar(100) not null unique,
    username varchar(100) not null unique,
    email varchar(255),
    role varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table book (
    id bigserial primary key,
    title varchar(255) not null,
    author varchar(255) not null,
    category varchar(100),
    isbn varchar(50),
    total_quantity int not null,
    available_quantity int not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint chk_book_quantities check (total_quantity >= 0 and available_quantity >= 0 and available_quantity <= total_quantity)
);

create table borrow_transaction (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    book_id bigint not null references book(id),
    borrowed_at timestamp not null,
    due_at timestamp not null,
    returned_at timestamp null,
    status varchar(30) not null
);

create table activity_log (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    book_id bigint references book(id),
    activity_type varchar(30) not null,
    message varchar(500) not null,
    occurred_at timestamp not null
);

create index idx_book_title on book(title);
create index idx_book_author on book(author);
create index idx_book_category on book(category);
create index idx_borrow_transaction_user_status on borrow_transaction(user_id, status);
create index idx_borrow_transaction_book_status on borrow_transaction(book_id, status);
create index idx_activity_log_user_occurred_at on activity_log(user_id, occurred_at desc);
