package com.example.library.circulation;

import java.time.Instant;

public record BookBorrowedEvent(
        Long actorUserId,
        String actorUsername,
        Long targetUserId,
        String targetUsername,
        Long bookId,
        String bookTitle,
        boolean fromReadyReservation,
        Instant occurredAt) {
}
