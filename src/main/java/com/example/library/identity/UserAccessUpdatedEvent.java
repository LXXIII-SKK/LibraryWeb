package com.example.library.identity;

import java.time.Instant;

public record UserAccessUpdatedEvent(
        Long actorUserId,
        String actorUsername,
        Long targetUserId,
        String targetUsername,
        String message,
        Instant occurredAt) {

    public UserAccessUpdatedEvent(
            Long actorUserId,
            String actorUsername,
            Long targetUserId,
            String targetUsername,
            String message) {
        this(actorUserId, actorUsername, targetUserId, targetUsername, message, Instant.now());
    }
}
