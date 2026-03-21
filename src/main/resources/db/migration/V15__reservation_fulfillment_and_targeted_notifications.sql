alter table reservation
    add column pickup_branch_id bigint references library_branch(id),
    add column reserved_holding_id bigint references book_holding(id),
    add column transfer_requested_at timestamp with time zone,
    add column ready_at timestamp with time zone,
    add column expires_at timestamp with time zone;

update reservation r
set pickup_branch_id = coalesce(u.home_branch_id, u.branch_id)
from app_user u
where r.user_id = u.id
  and r.pickup_branch_id is null;

create index idx_reservation_pickup_branch_status on reservation(pickup_branch_id, status);
create index idx_reservation_reserved_holding on reservation(reserved_holding_id);

alter table staff_notification
    add column target_user_id bigint references app_user(id) on delete cascade;

create index idx_staff_notification_target_user on staff_notification(target_user_id);
