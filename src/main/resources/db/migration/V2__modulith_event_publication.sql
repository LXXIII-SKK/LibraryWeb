create table event_publication (
    id uuid primary key,
    publication_date timestamp with time zone not null,
    listener_id varchar(255) not null,
    serialized_event text not null,
    event_type varchar(255) not null,
    completion_date timestamp with time zone null,
    last_resubmission_date timestamp with time zone null,
    completion_attempts int not null default 0,
    status varchar(30) not null
);

create table event_publication_archive (
    id uuid primary key,
    publication_date timestamp with time zone not null,
    listener_id varchar(255) not null,
    serialized_event text not null,
    event_type varchar(255) not null,
    completion_date timestamp with time zone null,
    last_resubmission_date timestamp with time zone null,
    completion_attempts int not null default 0,
    status varchar(30) not null
);

create index idx_event_publication_status on event_publication(status);
create index idx_event_publication_completion_date on event_publication(completion_date);
