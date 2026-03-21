package com.example.library.identity;

import jakarta.validation.constraints.NotNull;

public record UserAccessUpdateRequest(
        @NotNull AppRole role,
        @NotNull AccountStatus accountStatus,
        @NotNull MembershipStatus membershipStatus,
        Long branchId,
        Long homeBranchId) {
}
