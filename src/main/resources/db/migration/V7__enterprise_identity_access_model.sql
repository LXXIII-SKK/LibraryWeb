alter table app_user alter column role type varchar(30);

alter table app_user
    add column account_status varchar(30) not null default 'ACTIVE',
    add column membership_status varchar(30) not null default 'GOOD_STANDING',
    add column branch_id bigint,
    add column home_branch_id bigint;

update app_user
set role = 'MEMBER'
where role = 'USER';

update app_user
set branch_id = 1,
    home_branch_id = 1
where id in (1, 2, 3, 101, 102);

update app_user
set branch_id = 2,
    home_branch_id = 2
where id = 103;

update app_user
set branch_id = 99,
    home_branch_id = 99
where id = 104;
