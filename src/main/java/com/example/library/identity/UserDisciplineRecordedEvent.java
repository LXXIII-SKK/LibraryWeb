package com.example.library.identity;

import java.time.Instant;

public record UserDisciplineRecordedEvent(
        Long actorUserId,
        String actorUsername,
        Long targetUserId,
        String targetUsername,
        UserDisciplineActionType action,
        UserDisciplineReason reason,
        String message,
        Instant occurredAt) {
}
