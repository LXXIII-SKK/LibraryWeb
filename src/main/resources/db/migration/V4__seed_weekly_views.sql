insert into activity_log (user_id, book_id, activity_type, message, occurred_at)
values
    (1, 8, 'VIEWED', 'alina.reader viewed "Release It!"', current_timestamp - interval '1 day'),
    (2, 2, 'VIEWED', 'hoang.nguyen viewed "Designing Data-Intensive Applications"', current_timestamp - interval '2 days'),
    (3, 1, 'VIEWED', 'maya.tran viewed "Domain-Driven Design"', current_timestamp - interval '3 days'),
    (1, 8, 'VIEWED', 'alina.reader viewed "Release It!"', current_timestamp - interval '5 hours'),
    (2, 8, 'VIEWED', 'hoang.nguyen viewed "Release It!"', current_timestamp - interval '7 hours');
