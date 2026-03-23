package com.example.library.common;

import java.time.Instant;

public record OperationalActivityEvent(
        Long actorUserId,
        String activityType,
        String message,
        Instant occurredAt) {
}
