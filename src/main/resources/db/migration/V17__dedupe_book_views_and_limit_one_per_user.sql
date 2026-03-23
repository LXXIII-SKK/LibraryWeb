WITH ranked_views AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, book_id
               ORDER BY occurred_at DESC, id DESC
           ) AS row_num
    FROM activity_log
    WHERE activity_type = 'VIEWED'
      AND book_id IS NOT NULL
)
DELETE FROM activity_log
WHERE id IN (
    SELECT id
    FROM ranked_views
    WHERE row_num > 1
);

CREATE UNIQUE INDEX ux_activity_log_viewed_user_book_once_per_reset
    ON activity_log (user_id, book_id)
    WHERE activity_type = 'VIEWED'
      AND book_id IS NOT NULL;

CREATE INDEX idx_activity_log_viewed_book
    ON activity_log (book_id)
    WHERE activity_type = 'VIEWED'
      AND book_id IS NOT NULL;
