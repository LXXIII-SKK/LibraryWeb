package com.example.library.circulation;

import java.time.Instant;

public record ReservationCreatedEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        Instant occurredAt) {
}
