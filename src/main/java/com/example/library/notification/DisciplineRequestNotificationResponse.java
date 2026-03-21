package com.example.library.notification;

import java.time.Instant;
import java.util.List;

import com.example.library.identity.UserDisciplineActionType;
import com.example.library.identity.UserDisciplineReason;

public record DisciplineRequestNotificationResponse(
        String targetUsername,
        UserDisciplineActionType action,
        UserDisciplineReason reason,
        String note,
        List<String> notifiedRecipients,
        Instant createdAt) {
}
