package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

public record FineWaivedEvent(
        Long actorUserId,
        String actorUsername,
        Long bookId,
        String bookTitle,
        String targetUsername,
        BigDecimal amount,
        String note,
        Instant occurredAt) {
}
