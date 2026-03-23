package com.example.library.circulation;

import java.time.Instant;

public record BorrowingExceptionRecordedEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        String targetUsername,
        BorrowStatus status,
        String note,
        Instant occurredAt) {
}
