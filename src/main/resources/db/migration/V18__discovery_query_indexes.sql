CREATE INDEX idx_borrow_transaction_borrowed_at_book_user
    ON borrow_transaction (borrowed_at DESC, book_id, user_id);

CREATE INDEX idx_activity_log_viewed_occurred_at_book
    ON activity_log (occurred_at DESC, book_id)
    WHERE activity_type = 'VIEWED'
      AND book_id IS NOT NULL;
