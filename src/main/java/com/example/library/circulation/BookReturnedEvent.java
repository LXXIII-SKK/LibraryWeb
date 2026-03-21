package com.example.library.circulation;

import java.time.Instant;

public record BookReturnedEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        String targetUsername,
        Instant occurredAt) {
}
