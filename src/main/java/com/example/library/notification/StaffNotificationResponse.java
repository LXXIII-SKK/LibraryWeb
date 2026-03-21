package com.example.library.notification;

import java.time.Instant;
import java.util.Set;

import com.example.library.branch.BranchSummaryResponse;
import com.example.library.identity.AppRole;

public record StaffNotificationResponse(
        Long id,
        String title,
        String message,
        BranchSummaryResponse branch,
        Long targetUserId,
        String targetUsername,
        Set<AppRole> targetRoles,
        String createdByUsername,
        Instant createdAt,
        Instant readAt) {

    static StaffNotificationResponse from(StaffNotification notification, StaffNotificationReceipt receipt) {
        return new StaffNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                BranchSummaryResponse.from(notification.getBranch()),
                notification.getTargetUser() != null ? notification.getTargetUser().getId() : null,
                notification.getTargetUser() != null ? notification.getTargetUser().getUsername() : null,
                notification.getTargetRoles(),
                notification.getCreatedByUser().getUsername(),
                notification.getCreatedAt(),
                receipt != null ? receipt.getReadAt() : null);
    }
}
