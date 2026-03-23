package com.example.library.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffRegistrationRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotNull AppRole role,
        @NotNull AccountStatus accountStatus,
        Long branchId,
        Long homeBranchId,
        boolean requirePasswordChange) {
}
