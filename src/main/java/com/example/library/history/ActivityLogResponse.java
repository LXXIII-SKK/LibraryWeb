package com.example.library.history;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        Long userId,
        String username,
        Long bookId,
        String bookTitle,
        ActivityType activityType,
        String message,
        Instant occurredAt) {

    static ActivityLogResponse from(ActivityLog activityLog) {
        return new ActivityLogResponse(
                activityLog.getId(),
                activityLog.getUser().getId(),
                activityLog.getUser().getUsername(),
                activityLog.getBook() != null ? activityLog.getBook().getId() : null,
                activityLog.getBook() != null ? activityLog.getBook().getTitle() : null,
                activityLog.getActivityType(),
                activityLog.getMessage(),
                activityLog.getOccurredAt());
    }
}
