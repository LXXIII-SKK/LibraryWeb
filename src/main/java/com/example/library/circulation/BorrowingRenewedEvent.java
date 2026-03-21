package com.example.library.circulation;

import java.time.Instant;

public record BorrowingRenewedEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        String targetUsername,
        Instant newDueAt,
        Instant occurredAt) {
}
