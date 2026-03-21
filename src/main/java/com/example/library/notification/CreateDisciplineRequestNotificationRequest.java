package com.example.library.notification;

import com.example.library.identity.UserDisciplineActionType;
import com.example.library.identity.UserDisciplineReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDisciplineRequestNotificationRequest(
        @NotBlank @Size(max = 100) String targetUsername,
        @NotNull UserDisciplineActionType action,
        @NotNull UserDisciplineReason reason,
        @Size(max = 500) String note) {
}
