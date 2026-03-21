package com.example.library.circulation;

import java.time.Instant;

public record ReservationNoShowEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        String targetUsername,
        Instant occurredAt) {
}
