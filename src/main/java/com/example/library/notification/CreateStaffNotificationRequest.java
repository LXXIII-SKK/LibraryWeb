package com.example.library.notification;

import java.util.Set;

import com.example.library.identity.AppRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateStaffNotificationRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 600) String message,
        Long branchId,
        @NotEmpty Set<AppRole> targetRoles) {
}
