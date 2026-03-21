package com.example.library.circulation;

import java.time.Instant;

public record PolicyUpdatedEvent(
        Long actorUserId,
        String actorUsername,
        Instant occurredAt) {
}
