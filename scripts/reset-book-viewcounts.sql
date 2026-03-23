WITH deleted AS (
    DELETE FROM activity_log
    WHERE activity_type = 'VIEWED'
    RETURNING book_id
)
SELECT COUNT(*) AS deleted_view_events,
       COUNT(DISTINCT book_id) AS affected_books
FROM deleted;
