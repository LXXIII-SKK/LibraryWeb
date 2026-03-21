package com.example.library.identity;

import java.util.List;

import com.example.library.branch.BranchSummaryResponse;

public record UserAccessResponse(
        Long id,
        String username,
        String email,
        AppRole role,
        AccountStatus accountStatus,
        MembershipStatus membershipStatus,
        AccessScope scope,
        Long branchId,
        Long homeBranchId,
        BranchSummaryResponse branch,
        BranchSummaryResponse homeBranch,
        List<String> permissions) {

    static UserAccessResponse from(AppUser user) {
        return from(user, true);
    }

    static UserAccessResponse from(AppUser user, boolean includePermissions) {
        return new UserAccessResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getMembershipStatus(),
                user.getRole().scope(),
                user.getBranchId(),
                user.getHomeBranchId(),
                BranchSummaryResponse.from(user.getBranch()),
                BranchSummaryResponse.from(user.getHomeBranch()),
                includePermissions
                        ? user.getRole().permissions().stream()
                                .map(Enum::name)
                                .sorted()
                                .toList()
                        : List.of());
    }
}
